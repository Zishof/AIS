package ais.database.dao;

import ais.database.model.Kkn;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Kkn} (data program KKN / Kuliah Kerja
 * Nyata). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface KknDao extends GenericDao<Kkn, Long> {
    

}
