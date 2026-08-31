package ais.database.dao;

import ais.database.model.StatusKerjasamaMahasiswa;


/**
 * Kontrak DAO untuk entitas {@link ais.database.model.StatusKerjasamaMahasiswa} (data status
 * kerjasama mahasiswa). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface StatusKerjasamaMahasiswaDao extends GenericDao<StatusKerjasamaMahasiswa, Long> {
    

}
