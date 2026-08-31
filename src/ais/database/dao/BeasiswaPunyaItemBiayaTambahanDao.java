package ais.database.dao;

import ais.database.model.BeasiswaPunyaItemBiayaTambahan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.BeasiswaPunyaItemBiayaTambahan} (relasi
 * suatu beasiswa dengan item biaya tambahan yang ditanggungnya). Pasangan Dao/DaoImpl ini murni
 * memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat
 * javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface BeasiswaPunyaItemBiayaTambahanDao extends GenericDao<BeasiswaPunyaItemBiayaTambahan, Long> {
    

}
