package ais.database.dao;

import ais.database.model.NilaiHuruf;


/**
 * Kontrak DAO untuk entitas {@link ais.database.model.NilaiHuruf} (data referensi nilai
 * huruf). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface NilaiHurufDao extends GenericDao<NilaiHuruf, Long> {
    

}
