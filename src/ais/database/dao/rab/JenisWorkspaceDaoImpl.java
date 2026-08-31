package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.JenisWorkspace;

/**
 * Implementasi Hibernate untuk {@link JenisWorkspaceDao}, mengelola entitas
 * {@link ais.database.model.rab.JenisWorkspace}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class JenisWorkspaceDaoImpl extends
		GenericHibernateDao<JenisWorkspace, Long, JenisWorkspaceDao> implements
		JenisWorkspaceDao {

}
