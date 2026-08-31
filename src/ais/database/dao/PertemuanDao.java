package ais.database.dao;

import ais.database.model.Pertemuan;


/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Pertemuan} (data pertemuan
 * perkuliahan). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface PertemuanDao extends GenericDao<Pertemuan, Long> {
    

}
