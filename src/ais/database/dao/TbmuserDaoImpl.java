package ais.database.dao;


import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;


/**
 * Implementasi {@link TbmuserDao} — DAO untuk {@link Tbmuser}, entitas akun pengguna inti AIS
 * (login/autentikasi). Seperti dijelaskan di Javadoc {@link TbmuserDao}, kelas ini BUKAN pasangan
 * Dao/DaoImpl kosong biasa (bandingkan dengan mayoritas {@code XxxDaoImpl} lain di paket ini yang
 * hanya {@code extends} {@link GenericHibernateDao} tanpa isi — lihat Javadoc
 * {@link GenericHibernateDao} untuk perilaku CRUD generik yang tetap berlaku di sini, mis.
 * {@code findById}, {@code save}, {@code search}) — kelas ini menambah lima method autentikasi
 * yang seluruhnya membangun {@link Criteria} manual dengan {@link Restrictions#eq} pada kolom
 * {@code userId}+{@code userPassword}.
 *
 * <h2>CATATAN KEAMANAN (dilaporkan, TIDAK diperbaiki sesuai instruksi tugas dokumentasi ini)</h2>
 * <p>
 * Seluruh method di kelas ini memverifikasi kredensial lewat kesetaraan SQL biasa
 * ({@code Restrictions.eq("userPassword", ...)}) terhadap nilai {@link Tbmuser#getUserPassword()}
 * yang dikirim pemanggil — BUKAN membandingkan hash password dengan algoritma verifikasi khusus
 * password (mis. bcrypt/argon2 constant-time compare). Dari {@link Tbmuser#getUserPassword()}
 * (lihat {@code ais.database.model.Tbmuser}), nilai yang tersimpan di kolom ini pada beberapa alur
 * memakai {@code Common.desEncrypter} (DES, algoritma enkripsi SIMETRIS/reversibel, BUKAN fungsi
 * hash satu-arah) — bukan skema hashing password modern. Ini adalah pengamatan atas kode yang
 * sudah ada, dilaporkan sesuai instruksi tugas dokumentasi, TIDAK diubah di sini.
 * </p>
 *
 * <h2>Dua varian sesi: "biasa" vs "NewSession"</h2>
 * <p>
 * {@link #login(Tbmuser)} dan {@link #loadByUsernameAndPass(Tbmuser)} memakai
 * {@link #createCriteria()} bawaan (sesi DAO ini, lihat {@link GenericHibernateDao#getSession()}),
 * TANPA filter status aktif — akun nonaktif tetap bisa lolos. {@link #loginWithNewSession(Tbmuser)}
 * dan {@link #loadByUsernameAndPassWithNewSession(Tbmuser)} justru mengambil
 * {@link Session} BARU langsung dari {@link HibernateUtil#currentSession()} (bukan lewat
 * {@link #getSession()} DAO ini) DAN menambahkan filter {@code aktif IS NULL OR aktif = true} —
 * dipakai pada alur login utama AIS yang harus menolak akun nonaktif.
 * </p>
 */
public class TbmuserDaoImpl extends GenericHibernateDao<Tbmuser, Long, TbmuserDao> implements TbmuserDao {
    /**
     * Memeriksa kredensial TANPA filter status aktif (lihat "Dua varian sesi" pada Javadoc kelas)
     * lewat {@code COUNT(*)} — lebih murah dari {@link #loadByUsernameAndPass(Tbmuser)} karena
     * tidak memuat seluruh entitas, hanya menghitung baris yang cocok.
     *
     * @param users template berisi {@code userId} dan {@code userPassword} yang dicocokkan
     * @return {@code true} bila ada tepat satu atau lebih baris yang cocok persis
     */
    public Boolean login(Tbmuser users) {
        Criteria criteria = createCriteria();
        criteria.setProjection(Projections.rowCount());
        criteria.add(Restrictions.eq("userId", users.getUserId()));
        criteria.add(Restrictions.eq("userPassword", users.getUserPassword()));
        return ((Number) criteria.uniqueResult()).intValue() != 0;
    }

    /**
     * Sama seperti {@link #login(Tbmuser)} tapi mengembalikan entitas {@link Tbmuser} yang cocok
     * (bukan hanya boolean), TANPA filter status aktif.
     *
     * @param users template berisi {@code userId} dan {@code userPassword} yang dicocokkan
     * @return entitas {@link Tbmuser} pertama yang cocok, atau {@code null} bila tidak ada
     */
    public Tbmuser loadByUsernameAndPass(Tbmuser users) {
        Criteria criteria = createCriteria();
        criteria.add(Restrictions.eq("userId", users.getUserId()));
        criteria.add(Restrictions.eq("userPassword", users.getUserPassword()));
        return ((Tbmuser) criteria.uniqueResult());
    }

    /**
     * @param users template berisi {@code userId} yang diperiksa keberadaannya (password diabaikan
     *              di query ini meski dibawa parameter {@code Tbmuser}), TANPA filter status aktif
     * @return {@code true} bila sudah ada akun dengan {@code userId} tsb
     */
    public Boolean isExist(Tbmuser users) {
        Criteria criteria = createCriteria();
        criteria.add(Restrictions.eq("userId", users.getUserId()));
        return criteria.setMaxResults(ais.common.Common.MAX_RESULT).list().size() != 0;
    }

    /**
     * Varian {@link #loadByUsernameAndPass(Tbmuser)} yang HANYA mencocokkan akun dengan
     * {@code aktif} null-atau-true, dan memakai {@link Session} baru dari
     * {@link HibernateUtil#currentSession()} langsung (bukan sesi DAO ini) — lihat "Dua varian
     * sesi" pada Javadoc kelas. Dipakai pada alur login utama AIS.
     *
     * @param users template berisi {@code userId} dan {@code userPassword} yang dicocokkan
     * @return entitas {@link Tbmuser} yang cocok dan berstatus aktif, atau {@code null} bila tidak ada
     */
    public Tbmuser loadByUsernameAndPassWithNewSession(Tbmuser users) {
    	Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        criteria.add(Restrictions.eq("userId", users.getUserId()));
        criteria.add(Restrictions.eq("userPassword", users.getUserPassword()));
        Tbmuser users2 = ((Tbmuser) criteria.uniqueResult());
        return users2;
    }

	/**
	 * Varian {@link #login(Tbmuser)} yang HANYA menghitung akun dengan {@code aktif}
	 * null-atau-true, dan memakai {@link Session} baru dari {@link HibernateUtil#currentSession()}
	 * langsung (bukan sesi DAO ini) — lihat "Dua varian sesi" pada Javadoc kelas.
	 *
	 * @param users template berisi {@code userId} dan {@code userPassword} yang dicocokkan
	 * @return {@code true} bila ada akun aktif dengan kredensial yang cocok persis
	 */
	@Override
	public Boolean loginWithNewSession(Tbmuser users) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        criteria.setProjection(Projections.rowCount());
        criteria.add(Restrictions.eq("userId", users.getUserId()));
        criteria.add(Restrictions.eq("userPassword", users.getUserPassword()));
        Boolean result =  ((Number) criteria.uniqueResult()).intValue() != 0;
        return result;
	}


}
