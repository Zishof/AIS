package ais.common.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import org.hibernate.Session;
import org.hibernate.SQLQuery;
import ais.action.servlet.api.ApiTokenManager;
import ais.action.master.helper.generic.AmbilDataLampiranFileLain;
import ais.database.model.LogLogin;
import ais.database.model.file.FotoMahasiswaLulus;
import ais.database.model.koperasi.AnggotaKoperasi;

/** Pengujian offline tanpa koneksi atau perubahan data produksi. */
public final class Ecampus21RegressionSelfTest {
    public static final class TokenUser {
        public String getToken() { return "offline-regression-token"; }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        FotoMahasiswaLulus foto = new FotoMahasiswaLulus();
        AmbilDataLampiranFileLain.mappingInstanceData(foto, Long.valueOf(17), null,
                "foto", null, null, "image/jpeg", "test", "Test");
        check(foto.getNama() != null && foto.getNama().length() > 0, "Nama wajib tidak null");
        check(Long.valueOf(17).equals(foto.getMahasiswa()), "Pemilik foto dipertahankan");
        AmbilDataLampiranFileLain.mappingInstanceData(foto, Long.valueOf(17), null,
                "foto", null, "wisuda.jpg", "image/jpeg", "test", "Test");
        check("wisuda.jpg".equals(foto.getNama()), "Nama asli dipertahankan");

        String[] fields = { "getNama", "getDescription", "getIp", "getHostname",
                "getSessionid", "getKeterangan", "getOleh", "getOlehId" };
        for (String field : fields) {
            Column column = LogLogin.class.getMethod(field).getAnnotation(Column.class);
            check(column != null && "text".equals(column.columnDefinition()), "Mapping text: " + field);
        }
        String longText = new String(new char[1000]).replace('\0', 'x');
        LogLogin login = new LogLogin();
        login.setDescription(longText);
        check(longText.equals(login.getDescription()), "Isi log tidak dipotong");

        final List<String> calls = new ArrayList<String>();
        final TokenUser user = new TokenUser();
        Session session = (Session) Proxy.newProxyInstance(Session.class.getClassLoader(),
                new Class[] { Session.class }, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] values) {
                        calls.add(method.getName());
                        if ("get".equals(method.getName())) return user;
                        if ("clear".equals(method.getName())) {
                            check(!ApiTokenManager.containsToken(user.getToken()), "Detach sebelum publikasi token");
                        }
                        return null;
                    }
                });
        Method load = ApiTokenManager.class.getDeclaredMethod("loadSingleToken", Class.class, Long.class, Session.class);
        load.setAccessible(true);
        load.invoke(null, TokenUser.class, Long.valueOf(1), session);
        check(ApiTokenManager.getTokenValue(user.getToken()) == user, "Token tetap tersedia");
        check(calls.size() == 2 && "get".equals(calls.get(0)) && "clear".equals(calls.get(1)),
                "Session pemuat tidak dibagikan atau ditutup oleh pemuat satu token");
        ApiTokenManager.removeToken(user.getToken());

        final RuntimeException insertFailure = new IllegalStateException("simulated insert failure");
        final int[] selects = { 0 };
        final SQLQuery query = (SQLQuery) Proxy.newProxyInstance(SQLQuery.class.getClassLoader(),
                new Class[] { SQLQuery.class }, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] values) {
                        if ("uniqueResult".equals(method.getName())) { selects[0]++; return null; }
                        if ("executeUpdate".equals(method.getName())) throw insertFailure;
                        return proxy;
                    }
                });
        Session counterSession = (Session) Proxy.newProxyInstance(Session.class.getClassLoader(),
                new Class[] { Session.class }, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] values) {
                        if ("createSQLQuery".equals(method.getName())) return query;
                        throw new AssertionError("Unexpected session call: " + method.getName());
                    }
                });
        Method find = AnggotaKoperasi.class.getDeclaredMethod("cariAtauBuatKodeCounterId", Session.class,
                Long.TYPE, Long.TYPE);
        find.setAccessible(true);
        try {
            find.invoke(null, counterSession, Long.valueOf(0), Long.valueOf(0));
            throw new AssertionError("INSERT failure must propagate");
        } catch (InvocationTargetException expected) {
            check(expected.getCause() == insertFailure, "Penyebab asli dipertahankan");
        }
        check(selects[0] == 1, "Tidak query ulang setelah transaksi gagal");
        System.out.println("PASS Ecampus21RegressionSelfTest");
    }
}
