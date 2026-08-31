package ais.database.dao;

import ais.database.model.BadanHukum;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.BadanHukum} (data badan hukum/yayasan
 * penyelenggara). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface BadanHukumDao extends GenericDao<BadanHukum, Long>{

}
