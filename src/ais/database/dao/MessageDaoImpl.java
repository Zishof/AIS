package ais.database.dao;

import ais.database.model.Message;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Message} (data pesan). Kelas ini
 * murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class MessageDaoImpl extends
		GenericHibernateDao<Message, Long, MessageDao> implements MessageDao {

}
