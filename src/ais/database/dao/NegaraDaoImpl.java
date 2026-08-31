package ais.database.dao;

import ais.database.model.Negara;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Negara} (data referensi negara).
 * Kelas ini murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class NegaraDaoImpl extends GenericHibernateDao<Negara, Long, NegaraDao> implements NegaraDao{

}
