package ais.database.dao;

import ais.database.model.file.GambarFakultas;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.file.GambarFakultas} (data gambar/logo
 * fakultas). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface GambarFakultasDao extends GenericDao<GambarFakultas, Long>{

}
