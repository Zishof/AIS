package ais.database.dao;

import ais.database.model.JamPerkuliahan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.JamPerkuliahan} (data jam/slot waktu
 * perkuliahan). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface JamPerkuliahanDao extends GenericDao<JamPerkuliahan, Long> {

}
