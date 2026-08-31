package ais.database.dao.kedokteran;

import ais.database.dao.GenericDao;
import ais.database.model.kedokteran.PHDHasMahasiswa;

/**
 * DAO untuk entitas {@link ais.database.model.kedokteran.PHDHasMahasiswa} — relasi kepesertaan
 * mahasiswa pada {@link ais.database.model.kedokteran.PertemuanHasDosen} (modul pendidikan
 * kedokteran). Interface ini sengaja kosong: seluruh kontrak CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public interface PHDHasMahasiswaDao extends GenericDao<PHDHasMahasiswa, Long> {

}
