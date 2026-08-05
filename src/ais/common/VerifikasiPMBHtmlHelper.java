package ais.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSeleksi;
import ais.database.model.VerifikasiKelengkapanCalonMahasiswa;

public class VerifikasiPMBHtmlHelper {

    /**
     * MENGAMBIL DATA UNTUK DI-RENDER DI JSP
     * Mengembalikan List array object: [0] = VerifikasiKelengkapan, [1] = Data Existing (Bisa Null)
     */
    public static List<Object[]> getDaftarVerifikasi(BiodataCalonMahasiswa biodata, GelombangPendaftaran gelombang, JenisSeleksi jenisSeleksi, Session session) {
        Set<VerifikasiKelengkapanCalonMahasiswa> syaratSet = new HashSet<VerifikasiKelengkapanCalonMahasiswa>();
        boolean closeSession = false;

        try {
            if (session == null || !session.isOpen()) {
                session = HibernateUtil.openSession();
                closeSession = true;
            }

            // 1. Ambil syarat via tabel join agar tidak memicu LazyInitializationException
            // pada koleksi many-to-many GelombangPendaftaran/JenisSeleksi.
            if (gelombang != null && gelombang.getId() != null) {
                tambahVerifikasiDariJoin(session, syaratSet,
                        "select verifikasi from gelombang_punya_verifikasi where gelombang = :id",
                        gelombang.getId());
            }

            // 2. Ambil syarat jenis seleksi langsung dari join table.
            if (jenisSeleksi != null && jenisSeleksi.getId() != null) {
                tambahVerifikasiDariJoin(session, syaratSet,
                        "select verifikasi from jenis_seleksi_punya_verifikasi where jenis_seleksi = :id",
                        jenisSeleksi.getId());
            }

            if (syaratSet.isEmpty()) return new ArrayList<Object[]>();

            List<VerifikasiKelengkapanCalonMahasiswa> listVerifikasi = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>(syaratSet);
            Collections.sort(listVerifikasi); 

            // 3. Ambil data verifikasi existing berdasarkan ID Calon Mahasiswa
            Map<Long, BiodataCalonMahasiswaPunyaVerifikasiBerkas> existingData = new HashMap<Long, BiodataCalonMahasiswaPunyaVerifikasiBerkas>();
            if (biodata != null && biodata.getId() != null) {
                @SuppressWarnings("unchecked")
                List<BiodataCalonMahasiswaPunyaVerifikasiBerkas> listExist = session.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class)
                        .add(Restrictions.eq("biodataCalonMahasiswa.id", biodata.getId())).list();
                
                for (BiodataCalonMahasiswaPunyaVerifikasiBerkas berkas : listExist) {
                    if (berkas.getVerifikasiKelengkapanCalonMahasiswa() != null) {
                        existingData.put(berkas.getVerifikasiKelengkapanCalonMahasiswa().getId(), berkas);
                    }
                }
            }

            // 4. Mapping Data Terpadu
            List<Object[]> resultList = new ArrayList<Object[]>();
            for(VerifikasiKelengkapanCalonMahasiswa v : listVerifikasi) {
                resultList.add(new Object[] { v, existingData.get(v.getId()) });
            }
            return resultList;

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/VerifikasiPMBHtmlHelper.java:74");
        } finally {
            if (closeSession) {
                HibernateUtil.closeSessionQuietly(session);
            }
        }
        return new ArrayList<Object[]>();
    }

    @SuppressWarnings("unchecked")
    private static void tambahVerifikasiDariJoin(Session session,
            Set<VerifikasiKelengkapanCalonMahasiswa> syaratSet, String sql, Long id) {
        if (session == null || syaratSet == null || sql == null || id == null) {
            return;
        }
        List<Object> ids = session.createSQLQuery(sql).setLong("id", id.longValue()).list();
        for (Object objId : ids) {
            Long verifikasiId = null;
            if (objId instanceof Number) {
                verifikasiId = Long.valueOf(((Number) objId).longValue());
            } else if (objId != null) {
                try {
                    verifikasiId = Long.valueOf(objId.toString());
                } catch (Exception e) {
                    verifikasiId = null;
                }
            }
            if (verifikasiId != null) {
                VerifikasiKelengkapanCalonMahasiswa v = (VerifikasiKelengkapanCalonMahasiswa) session.get(
                        VerifikasiKelengkapanCalonMahasiswa.class, verifikasiId);
                if (v != null) {
                    syaratSet.add(v);
                }
            }
        }
    }

    /**
     * Menyimpan data verifikasi dari string raw (hasil parsing JavaScript).
     */
    public static void simpanVerifikasiHtml(BiodataCalonMahasiswa biodata, String rawVerifikasiData, Session session) {
        if (biodata == null || biodata.getId() == null || rawVerifikasiData == null || rawVerifikasiData.trim().isEmpty() || session == null) {
            return;
        }

        Transaction tx = null;
        boolean isNewTransaction = false;

        try {
            if (!session.getTransaction().isActive()) {
                tx = session.beginTransaction();
                isNewTransaction = true;
            }

            @SuppressWarnings("unchecked")
            List<BiodataCalonMahasiswaPunyaVerifikasiBerkas> existingList = session.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class)
                    .add(Restrictions.eq("biodataCalonMahasiswa.id", biodata.getId())).list();

            Map<Long, BiodataCalonMahasiswaPunyaVerifikasiBerkas> existingMap = new HashMap<Long, BiodataCalonMahasiswaPunyaVerifikasiBerkas>();
            for (BiodataCalonMahasiswaPunyaVerifikasiBerkas b : existingList) {
                if(b.getVerifikasiKelengkapanCalonMahasiswa() != null) {
                    existingMap.put(b.getVerifikasiKelengkapanCalonMahasiswa().getId(), b);
                }
            }

            // Parsing Raw Data: format "ID_SYARAT<=>BOOLEAN_CHECKED<=>KETERANGAN\n..."
            String[] rows = rawVerifikasiData.split("\n");
            for (String row : rows) {
                if (row.trim().isEmpty()) continue;

                String[] parts = row.split("<=>");
                if (parts.length >= 2) {
                    Long syaratId = Long.parseLong(parts[0].trim());
                    boolean isChecked = Boolean.parseBoolean(parts[1].trim());
                    String keterangan = parts.length > 2 ? parts[2].trim() : "";

                    BiodataCalonMahasiswaPunyaVerifikasiBerkas obj = existingMap.get(syaratId);
                    
                    if (obj == null) {
                        obj = new BiodataCalonMahasiswaPunyaVerifikasiBerkas();
                        
                        BiodataCalonMahasiswa bLoad = (BiodataCalonMahasiswa) session.get(BiodataCalonMahasiswa.class, biodata.getId());
                        obj.setBiodataCalonMahasiswa(bLoad);
                        
                        VerifikasiKelengkapanCalonMahasiswa syarat = (VerifikasiKelengkapanCalonMahasiswa) session.get(VerifikasiKelengkapanCalonMahasiswa.class, syaratId);
                        obj.setVerifikasiKelengkapanCalonMahasiswa(syarat);
                        
                        obj.setVerified(isChecked);
                        obj.setKeterangan(keterangan);
                        session.save(obj);
                    } else {
                        obj.setVerified(isChecked);
                        obj.setKeterangan(keterangan);
                        session.update(obj);
                    }
                }
            }

            if (isNewTransaction && tx != null) {
                tx.commit();
            }
        } catch (Exception e) {
            if (isNewTransaction && tx != null) {
                tx.rollback();
            }
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/VerifikasiPMBHtmlHelper.java:147");
        } 
    }
}
