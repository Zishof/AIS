package ais.database.dao;

import ais.database.model.DetailSettingBiaya;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.DetailSettingBiaya} (detail setting/aturan
 * biaya). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface DetailSettingBiayaDao extends GenericDao<DetailSettingBiaya, Long>{

}
