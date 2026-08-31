package ais.database.dao;

import ais.database.model.Ruang;


/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Ruang} (data referensi ruang kuliah).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface RuangDao extends GenericDao<Ruang, Long> {
    

}
