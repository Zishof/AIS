package ais.database.dao;

import ais.database.model.Propinsi;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Propinsi} (data referensi
 * propinsi). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PropinsiDaoImpl extends GenericHibernateDao<Propinsi, Long, PropinsiDao> implements PropinsiDao{

}
