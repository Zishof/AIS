package ais.database.dao;

import ais.database.model.Judisium;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Judisium} (data yudisium/predikat
 * kelulusan). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface JudisiumDao extends GenericDao<Judisium, Long> {

}
