package ais.database.dao;

import ais.database.model.QuotaWisudaUntukFakultas;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.QuotaWisudaUntukFakultas} (data
 * kuota wisuda per fakultas). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class QuotaWisudaUntukFakultasDaoImpl extends GenericHibernateDao<QuotaWisudaUntukFakultas, Long, QuotaWisudaUntukFakultasDao> implements QuotaWisudaUntukFakultasDao{

}
