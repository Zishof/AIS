package ais.database.dao;

import ais.database.model.Jenjang;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Jenjang} (data jenjang pendidikan, mis.
 * D3/S1/S2). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface JenjangDao extends GenericDao<Jenjang, Long> {

}
