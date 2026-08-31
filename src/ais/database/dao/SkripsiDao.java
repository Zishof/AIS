package ais.database.dao;

import ais.database.model.Skripsi;


/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Skripsi} (data skripsi mahasiswa).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface SkripsiDao extends GenericDao<Skripsi, Long> {
    

}
