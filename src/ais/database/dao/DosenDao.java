package ais.database.dao;

import ais.database.model.Dosen;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Dosen} (data dosen). Pasangan Dao/DaoImpl
 * ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa method tambahan --
 * lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
//Dosen  : nama entitynya sesuai dao
//long : tipe primary key
public interface DosenDao extends GenericDao<Dosen, Long> {
    

}
