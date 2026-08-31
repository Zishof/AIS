package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.Acara;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.Acara} (acara/kegiatan dalam modul RAB).
 * Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi CRUD generik
 * (simpan, hapus, cari, cari-berhalaman, pencarian bebas ilike) diwariskan langsung dari sana.
 */
public interface AcaraDao extends GenericDao<Acara, Long>{

}
