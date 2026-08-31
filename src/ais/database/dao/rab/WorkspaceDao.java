package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.Workspace;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.Workspace} (workspace/ruang kerja
 * kolaborasi RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh
 * operasi CRUD generik diwariskan langsung dari sana.
 */
public interface WorkspaceDao extends GenericDao<Workspace, Long>{

}
