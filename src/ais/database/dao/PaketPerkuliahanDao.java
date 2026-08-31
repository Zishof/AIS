package ais.database.dao;

import ais.database.model.PaketPerkuliahan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.PaketPerkuliahan} (data paket
 * perkuliahan). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface PaketPerkuliahanDao extends GenericDao<PaketPerkuliahan, Long>{

}
