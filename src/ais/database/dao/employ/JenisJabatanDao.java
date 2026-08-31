package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.JenisJabatan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.JenisJabatan} (jenis jabatan
 * pegawai). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi
 * CRUD generik diwariskan langsung dari sana.
 */
public interface JenisJabatanDao extends GenericDao<JenisJabatan, Long> {

}