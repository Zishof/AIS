package ais.database.dao;

import ais.database.model.TimDosen;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.TimDosen} (data tim dosen). Kelas
 * ini murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class TimDosenDaoImpl extends GenericHibernateDao<TimDosen, Long, TimDosenDao> implements TimDosenDao{

}
