package ais.database.dao;

import ais.database.model.Kelas;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Kelas} (data kelas perkuliahan). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface KelasDao extends GenericDao<Kelas, Long>{

}
