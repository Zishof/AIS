package ais.common.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.exception.JDBCConnectionException;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSeleksi;

/** Session/criteria palsu: tidak membutuhkan database atau cache master. */
public final class GelombangSessionSelfTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        final JenisSeleksi pilihan = new JenisSeleksi();
        pilihan.setId(1L);
        pilihan.setKode("A");
        final int[] queries = { 0 };
        final boolean[] fail = { false };
        final JDBCConnectionException failure = new JDBCConnectionException(
                "This connection has been closed", new SQLException("closed", "08003"));
        final Criteria criteria = (Criteria) Proxy.newProxyInstance(Criteria.class.getClassLoader(),
                new Class<?>[] { Criteria.class }, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] values) {
                        if ("uniqueResult".equals(method.getName())) {
                            queries[0]++;
                            if (fail[0]) throw failure;
                            return pilihan;
                        }
                        return proxy;
                    }
                });
        Session session = (Session) Proxy.newProxyInstance(Session.class.getClassLoader(),
                new Class<?>[] { Session.class }, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] values) {
                        check("createCriteria".equals(method.getName()),
                                "Must not close, commit or replace caller session: " + method.getName());
                        return criteria;
                    }
                });
        GelombangPendaftaran gelombang = new GelombangPendaftaran();
        check(gelombang.ambilJenisSeleksi(session).isEmpty(), "Empty configuration");
        check(queries[0] == 0, "No query for empty configuration");
        gelombang.setJenisSeleksi(pilihan);
        gelombang.setJenisSeleksiLain(" A, 1, , A ");
        List<JenisSeleksi> result = gelombang.ambilJenisSeleksi(session);
        check(result.size() == 1 && result.get(0) == pilihan, "Deduplicate main/additional choice");
        check(queries[0] == 3, "Read nonblank configured choices through caller session");
        fail[0] = true;
        queries[0] = 0;
        try {
            gelombang.ambilJenisSeleksi(session);
            throw new AssertionError("Database failure must not become incomplete choices");
        } catch (JDBCConnectionException expected) {
            check(expected == failure, "Preserve original database cause");
            check(queries[0] == 1, "Do not continue querying a failed connection");
        }
        System.out.println("PASS GelombangSessionSelfTest");
    }
}
