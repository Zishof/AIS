package ais.database.dao.kedokteran;

import ais.database.dao.GenericDao;
import ais.database.model.kedokteran.JenisPertemuan;

/**
 * DAO untuk entitas {@link ais.database.model.kedokteran.JenisPertemuan} — jenis pertemuan
 * kedokteran (mis. kuliah, praktikum, bimbingan klinik) pada modul pendidikan kedokteran.
 * Interface ini sengaja kosong: seluruh kontrak CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public interface JenisPertemuanDao extends GenericDao<JenisPertemuan, Long> {

}
