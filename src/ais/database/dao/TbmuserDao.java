package ais.database.dao;

import ais.database.model.Tbmuser;



/**
 * DAO untuk {@link Tbmuser} — entitas akun pengguna inti AIS (login/autentikasi), dipakai al. oleh
 * {@code ais.common.CommonSecurityLoginHelper} dan {@code ais.delivery.email.sender.MailHelper}.
 * BERBEDA dari mayoritas pasangan {@code XxxDao}/{@code XxxDaoImpl} lain di paket ini (lihat
 * Javadoc {@link GenericDao} untuk pola umumnya) — interface ini menambah lima method
 * autentikasi/pencarian khusus di luar {@link GenericDao} generik, karena login butuh query
 * kredensial (userId + password) yang tidak bisa dilayani method generik {@code find*}/{@code
 * search*} biasa. Seluruh perilaku CRUD generik lain (simpan, hapus, cari-by-id, dst.) tetap
 * berasal dari {@link GenericDao}/{@link GenericHibernateDao} seperti biasa — lihat implementasi
 * konkret di {@link TbmuserDaoImpl} untuk detail tiap method di bawah, termasuk CATATAN KEAMANAN
 * penting soal perbandingan password di sana.
 */
public interface TbmuserDao extends GenericDao<Tbmuser, Long> {
    /**
     * @param users template berisi {@code userId} dan {@code userPassword} yang akan dicocokkan
     * @return {@code true} bila ada baris {@code Tbmuser} dengan userId+password PERSIS SAMA
     *         (lihat catatan keamanan pada {@link TbmuserDaoImpl#login(Tbmuser)})
     */
    public Boolean login(Tbmuser users);

    /**
     * Sama seperti {@link #loadByUsernameAndPass(Tbmuser)}, tapi hanya mencocokkan pada baris
     * dengan {@code aktif} null-atau-true, dan memakai sesi Hibernate baru/terpisah — lihat
     * {@link TbmuserDaoImpl#loadByUsernameAndPassWithNewSession(Tbmuser)}.
     *
     * @param users template berisi {@code userId} dan {@code userPassword}
     * @return entitas {@link Tbmuser} yang cocok dan aktif, atau {@code null} bila tidak ada
     */
    public Tbmuser loadByUsernameAndPassWithNewSession(Tbmuser users);

    /**
     * Varian {@link #login(Tbmuser)} yang hanya menghitung baris dengan {@code aktif}
     * null-atau-true dan memakai sesi Hibernate baru/terpisah — lihat
     * {@link TbmuserDaoImpl#loginWithNewSession(Tbmuser)}.
     *
     * @param users template berisi {@code userId} dan {@code userPassword}
     * @return {@code true} bila ada akun aktif dengan kredensial yang cocok persis
     */
    public Boolean loginWithNewSession(Tbmuser users);

    /**
     * @param users template berisi {@code userId} dan {@code userPassword} yang akan dicocokkan
     * @return entitas {@link Tbmuser} pertama yang cocok, TANPA filter status aktif (berbeda dari
     *         {@link #loadByUsernameAndPassWithNewSession(Tbmuser)}), atau {@code null} bila tidak ada
     */
    public Tbmuser loadByUsernameAndPass(Tbmuser users);

    /**
     * @param users template berisi {@code userId} yang diperiksa keberadaannya (password diabaikan)
     * @return {@code true} bila sudah ada akun dengan {@code userId} tsb, TANPA filter status aktif
     */
    public Boolean isExist(Tbmuser users);

}
