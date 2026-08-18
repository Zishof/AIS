package ais.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.CalonSiswaPunyaVerifikasiBerkas;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa;

/**
 * Helper class untuk mengelola data Verifikasi Berkas Calon Siswa (PPDB) 
 * pada antarmuka berbasis HTML / JSP.
 */
public class VerifikasiPSBHtmlHelper {

    /**
     * Mengambil daftar kelengkapan berkas yang harus dipenuhi oleh calon siswa 
     * berdasarkan gelombang pendaftarannya, beserta status verifikasi saat ini.
     * * @param calonSiswa Objek CalonSiswa (wajib)
     * @param gelombang  Objek GelombangPendaftaranPsb dari calon siswa tersebut
     * @param session    Hibernate Session aktif yang di-*passing* dari JSP pemanggil
     * @return List of Object[]. Indeks 0 = VerifikasiKelengkapanCalonSiswa (Master), Indeks 1 = CalonSiswaPunyaVerifikasiBerkas (Data Siswa)
     */
    public static List<Object[]> getDaftarVerifikasi(CalonSiswa calonSiswa, GelombangPendaftaranPsb gelombang, Session session) {
        
        List<Object[]> resultList = new ArrayList<Object[]>();

        // Validasi parameter utama
        if (calonSiswa == null || calonSiswa.getId() == null || gelombang == null || session == null) {
            return resultList; 
        }

        try {
            // Ambil data gelombang pendaftaran terbaru untuk menghindari lazy loading issues
            GelombangPendaftaranPsb gelData = (GelombangPendaftaranPsb) session
                    .createCriteria(GelombangPendaftaranPsb.class)
                    .add(Restrictions.idEq(gelombang.getId()))
                    .uniqueResult();

            if (gelData == null || gelData.getVerifikasiKelengkapanCalonSiswas() == null) {
                return resultList;
            }

            // Ambil daftar master verifikasi berkas yang terikat pada gelombang ini
            List<VerifikasiKelengkapanCalonSiswa> listMasterVerifikasi = new ArrayList<VerifikasiKelengkapanCalonSiswa>(gelData.getVerifikasiKelengkapanCalonSiswas());
            
            try {
                Collections.sort(listMasterVerifikasi); // Urutkan berdasarkan ketentuan sorting kelas master
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/VerifikasiPSBHtmlHelper.java:55");}

            boolean perluUpdateDatabase = false;
            Transaction tx = null;

            for (VerifikasiKelengkapanCalonSiswa masterVerifikasi : listMasterVerifikasi) {
                
                // Lewati jika berkas ini sedang tidak diaktifkan oleh admin
                if (masterVerifikasi == null || masterVerifikasi.getAktif() == null || !masterVerifikasi.getAktif()) {
                    continue;
                }

                // Cari data transaksi berkas milik calon siswa ini
                CalonSiswaPunyaVerifikasiBerkas dataBerkasSiswa = (CalonSiswaPunyaVerifikasiBerkas) session
                        .createCriteria(CalonSiswaPunyaVerifikasiBerkas.class)
                        .add(Restrictions.eq("verifikasiKelengkapanCalonSiswa.id", masterVerifikasi.getId()))
                        .add(Restrictions.eq("calonSiswa.id", calonSiswa.getId()))
                        .setMaxResults(1)
                        .uniqueResult();

                // Jika data berkas belum ada di database siswa, maka inisialisasi / generate record baru
                if (dataBerkasSiswa == null) {
                    if (!perluUpdateDatabase) {
                        tx = session.beginTransaction();
                        perluUpdateDatabase = true;
                    }
                    
                    dataBerkasSiswa = new CalonSiswaPunyaVerifikasiBerkas();
                    dataBerkasSiswa.setCalonSiswa(calonSiswa);
                    dataBerkasSiswa.setVerifikasiKelengkapanCalonSiswa(masterVerifikasi);
                    dataBerkasSiswa.setVerified(false); // Default belum diverifikasi
                    dataBerkasSiswa.setKeterangan("");
                    
                    session.saveOrUpdate(dataBerkasSiswa);
                }

                // Masukkan ke dalam daftar kembalian untuk JSP
                Object[] rowData = new Object[2];
                rowData[0] = masterVerifikasi;
                rowData[1] = dataBerkasSiswa;
                
                resultList.add(rowData);
            }
            
            // Komit transaksi jika ada record baru yang disuntikkan ke database
            if (perluUpdateDatabase && tx != null && tx.isActive()) {
                tx.commit();
            }

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/VerifikasiPSBHtmlHelper.java:105");
        }

        return resultList;
    }
    
    // Overloading untuk penambahan parameter Jenis Seleksi (Jika sistem Anda menggunakannya di masa depan)
    public static List<Object[]> getDaftarVerifikasi(CalonSiswa calonSiswa, GelombangPendaftaranPsb gelombang, Object jenisSeleksi, Session session) {
    	// Anda bisa mengembangkan logika tambahan di sini jika jenisSeleksi ikut mempengaruhi syarat berkas
    	return getDaftarVerifikasi(calonSiswa, gelombang, session);
    }
}