package ais.database.dao;

import ais.database.model.StatusPegawai;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.StatusPegawai} (data referensi status
 * pegawai). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface StatusPegawaiDao extends GenericDao<StatusPegawai, Long> {

}
