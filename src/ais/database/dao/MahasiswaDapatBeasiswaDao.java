package ais.database.dao;

import ais.database.model.MahasiswaDapatBeasiswa;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.MahasiswaDapatBeasiswa} (relasi mahasiswa
 * penerima suatu beasiswa). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface MahasiswaDapatBeasiswaDao extends GenericDao<MahasiswaDapatBeasiswa, Long> {
    

}
