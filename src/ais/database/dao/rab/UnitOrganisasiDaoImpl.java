package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.UnitOrganisasi;

/**
 * Implementasi Hibernate untuk {@link UnitOrganisasiDao}, mengelola entitas
 * {@link ais.database.model.rab.UnitOrganisasi}. Kosong sesuai desain — seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class UnitOrganisasiDaoImpl extends
		GenericHibernateDao<UnitOrganisasi, Long, UnitOrganisasiDao> implements
		UnitOrganisasiDao {

}
