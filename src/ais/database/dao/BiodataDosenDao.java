package ais.database.dao;

import ais.database.model.BiodataDosen;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.BiodataDosen} (biodata dosen). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface BiodataDosenDao extends GenericDao<BiodataDosen, Long>{

}
