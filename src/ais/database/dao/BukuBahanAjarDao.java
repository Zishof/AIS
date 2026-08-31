package ais.database.dao;

import ais.database.model.BukuBahanAjar;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.BukuBahanAjar} (data buku bahan ajar).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface BukuBahanAjarDao extends GenericDao<BukuBahanAjar, Long> {

}
