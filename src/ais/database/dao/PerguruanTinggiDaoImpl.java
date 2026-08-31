package ais.database.dao;

import ais.database.model.PerguruanTinggi;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PerguruanTinggi} (data referensi
 * perguruan tinggi). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PerguruanTinggiDaoImpl extends GenericHibernateDao<PerguruanTinggi, Long, PerguruanTinggiDao> implements PerguruanTinggiDao{

}
