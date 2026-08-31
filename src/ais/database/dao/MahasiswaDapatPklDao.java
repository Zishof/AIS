package ais.database.dao;

import ais.database.model.MahasiswaDapatPkl;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.MahasiswaDapatPkl} (relasi mahasiswa
 * peserta PKL / Praktik Kerja Lapangan). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface MahasiswaDapatPklDao extends GenericDao<MahasiswaDapatPkl, Long> {
    

}
