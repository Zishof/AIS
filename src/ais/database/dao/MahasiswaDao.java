package ais.database.dao;

import ais.database.model.Mahasiswa;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Mahasiswa} (data mahasiswa). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface MahasiswaDao extends GenericDao<Mahasiswa, Long> {
    

}
