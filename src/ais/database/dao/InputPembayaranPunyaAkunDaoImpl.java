package ais.database.dao;

import ais.database.model.InputPembayaranPunyaAkun;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.InputPembayaranPunyaAkun}
 * (relasi input pembayaran dengan akun terkait), lewat
 * {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh perilaku
 * CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class InputPembayaranPunyaAkunDaoImpl extends GenericHibernateDao<InputPembayaranPunyaAkun, Long, InputPembayaranPunyaAkunDao> implements InputPembayaranPunyaAkunDao{

}
