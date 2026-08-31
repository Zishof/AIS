package ais.database.dao;

import ais.database.model.MatakuliahEkivalen;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.MatakuliahEkivalen} (data ekivalensi
 * mata kuliah). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface MatakuliahEkivalenDao extends
		GenericDao<MatakuliahEkivalen, Long> {

}
