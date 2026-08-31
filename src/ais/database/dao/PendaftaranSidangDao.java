package ais.database.dao;

import ais.database.model.PendaftaranSidang;


/**
 * Kontrak DAO untuk entitas {@link ais.database.model.PendaftaranSidang} (data pendaftaran
 * sidang). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface PendaftaranSidangDao extends GenericDao<PendaftaranSidang, Long> {
    

}
