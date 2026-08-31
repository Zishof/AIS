package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.JenisWorkspace;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.JenisWorkspace} (jenis workspace/ruang
 * kerja RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi
 * CRUD generik diwariskan langsung dari sana.
 */
public interface JenisWorkspaceDao extends GenericDao<JenisWorkspace, Long>{

}
