package ais.database.dao;

import ais.database.model.JadwalPembayaran;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.JadwalPembayaran} (jadwal pembayaran).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface JadwalPembayaranDao extends GenericDao<JadwalPembayaran, Long>{

}
