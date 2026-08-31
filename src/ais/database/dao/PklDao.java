package ais.database.dao;

import ais.database.model.Pkl;


/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Pkl} (data praktik kerja lapangan).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface PklDao extends GenericDao<Pkl, Long> {
    

}
