package ais.database.dao;

import ais.database.model.SettingBiaya;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.SettingBiaya} (data pengaturan biaya).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface SettingBiayaDao extends GenericDao<SettingBiaya, Long>{

}
