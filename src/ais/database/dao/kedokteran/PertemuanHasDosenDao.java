package ais.database.dao.kedokteran;

import ais.database.dao.GenericDao;
import ais.database.model.kedokteran.PertemuanHasDosen;

/**
 * DAO untuk entitas {@link ais.database.model.kedokteran.PertemuanHasDosen} — relasi dosen
 * pengajar/pembimbing pada suatu {@link ais.database.model.kedokteran.PertemuanKedokteran} (modul
 * pendidikan kedokteran). Interface ini sengaja kosong: seluruh kontrak CRUD generik diwariskan
 * dari {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public interface PertemuanHasDosenDao extends GenericDao<PertemuanHasDosen, Long> {

}
