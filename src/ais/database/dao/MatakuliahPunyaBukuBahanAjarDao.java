package ais.database.dao;

import ais.database.model.MatakuliahPunyaBukuBahanAjar;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.MatakuliahPunyaBukuBahanAjar} (data
 * relasi mata kuliah dengan buku bahan ajarnya). Pasangan Dao/DaoImpl ini murni memakai
 * perilaku generik {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc
 * di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface MatakuliahPunyaBukuBahanAjarDao extends
		GenericDao<MatakuliahPunyaBukuBahanAjar, Long> {

}
