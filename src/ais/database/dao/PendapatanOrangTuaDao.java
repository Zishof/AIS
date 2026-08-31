package ais.database.dao;

import ais.database.model.PendapatanOrangTua;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.PendapatanOrangTua} (data pendapatan
 * orang tua mahasiswa). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface PendapatanOrangTuaDao extends GenericDao<PendapatanOrangTua, Long>{

}
