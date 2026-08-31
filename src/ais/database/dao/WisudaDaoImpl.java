package ais.database.dao;

import ais.database.model.Wisuda;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Wisuda} (data wisuda). Kelas ini
 * murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class WisudaDaoImpl extends GenericHibernateDao<Wisuda, Long, WisudaDao> implements WisudaDao{

}
