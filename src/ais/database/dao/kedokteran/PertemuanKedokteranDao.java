package ais.database.dao.kedokteran;

import ais.database.dao.GenericDao;
import ais.database.model.kedokteran.PertemuanKedokteran;

/**
 * DAO untuk entitas {@link ais.database.model.kedokteran.PertemuanKedokteran} — pertemuan
 * akademik/klinik pada modul pendidikan kedokteran, entitas induk relasi dosen dan mahasiswa
 * peserta. Interface ini sengaja kosong: seluruh kontrak CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public interface PertemuanKedokteranDao extends GenericDao<PertemuanKedokteran, Long> {

}
