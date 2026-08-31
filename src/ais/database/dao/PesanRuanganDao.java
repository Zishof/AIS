package ais.database.dao;

import ais.database.model.PesanRuangan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.PesanRuangan} (data pemesanan ruangan).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface PesanRuanganDao extends GenericDao<PesanRuangan, Long> {

}
