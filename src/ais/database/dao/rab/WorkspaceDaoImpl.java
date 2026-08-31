package ais.database.dao.rab;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.rab.Workspace;

/**
 * Implementasi Hibernate untuk {@link WorkspaceDao}, mengelola entitas
 * {@link ais.database.model.rab.Workspace}. Kosong sesuai desain — seluruh logika CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class WorkspaceDaoImpl extends
		GenericHibernateDao<Workspace, Long, WorkspaceDao> implements
		WorkspaceDao {

}
