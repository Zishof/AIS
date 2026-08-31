package ais.database.dao;

import ais.database.model.Matakuliah;


/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Matakuliah} (data mata kuliah). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface MatakuliahDao extends GenericDao<Matakuliah, Long> {
    

}
