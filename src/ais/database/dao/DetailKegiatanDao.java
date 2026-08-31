package ais.database.dao;

import ais.database.model.DetailKegiatan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.DetailKegiatan} (detail kegiatan).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface DetailKegiatanDao extends GenericDao<DetailKegiatan, Long>{

}
