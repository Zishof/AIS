package ais.database.dao;

import ais.database.model.PekerjaanOrangTua;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.PekerjaanOrangTua} (data pekerjaan
 * orang tua mahasiswa). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface PekerjaanOrangTuaDao extends GenericDao<PekerjaanOrangTua, Long>{

}
