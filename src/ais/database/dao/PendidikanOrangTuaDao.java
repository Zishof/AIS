package ais.database.dao;

import ais.database.model.PendidikanOrangTua;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.PendidikanOrangTua} (data pendidikan
 * orang tua mahasiswa). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface PendidikanOrangTuaDao extends GenericDao<PendidikanOrangTua, Long>{

}
