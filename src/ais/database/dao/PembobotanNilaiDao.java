package ais.database.dao;

import ais.database.model.PembombotanNilai;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.PembombotanNilai} (data pembobotan
 * nilai). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface PembobotanNilaiDao extends GenericDao<PembombotanNilai, Long>{

}
