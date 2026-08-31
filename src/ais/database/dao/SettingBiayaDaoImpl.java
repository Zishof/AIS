package ais.database.dao;

import ais.database.model.SettingBiaya;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.SettingBiaya} (data pengaturan
 * biaya). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class SettingBiayaDaoImpl extends GenericHibernateDao<SettingBiaya, Long, SettingBiayaDao> implements SettingBiayaDao{

}
