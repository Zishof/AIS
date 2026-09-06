package ais.action.master;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;

import ais.database.model.Mahasiswa;
import ais.database.model.Skripsi;

/** Memeriksa kontrak query tanpa koneksi database atau perubahan data mahasiswa. */
public class SkripsiWisudaSelectionSelfTest {
    private static class Queries implements InvocationHandler {
        final List<String> calls = new ArrayList<String>();
        final Skripsi graded;
        final Skripsi fallback;
        int queries;

        Queries(Skripsi graded, Skripsi fallback) {
            this.graded = graded;
            this.fallback = fallback;
        }

        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("createCriteria".equals(name)) {
                require(args[0] == Skripsi.class, "Wrong entity");
                queries++;
                return Proxy.newProxyInstance(Criteria.class.getClassLoader(),
                        new Class<?>[] { Criteria.class }, this);
            }
            if ("uniqueResult".equals(name)) {
                return queries == 1 ? graded : fallback;
            }
            if ("add".equals(name) || "addOrder".equals(name) || "setMaxResults".equals(name)) {
                calls.add(queries + ":" + name + ":" + args[0]);
                return proxy;
            }
            throw new AssertionError("Unexpected call: " + name);
        }

        Session session() {
            return (Session) Proxy.newProxyInstance(Session.class.getClassLoader(),
                    new Class<?>[] { Session.class }, this);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        Mahasiswa mahasiswa = new Mahasiswa();
        Skripsi graded = new Skripsi();
        Skripsi fallback = new Skripsi();
        Queries q = new Queries(graded, fallback);
        require(PendaftaranWisudaMahasiswaAction.ambilSkripsiUntukWisuda(q.session(), mahasiswa)
                == graded, "Graded thesis must retain priority");
        require(q.queries == 1, "Fallback should not run for graded thesis");
        require(q.calls.contains("1:add:totalNilai>0.1"), "Grade criterion missing");
        q = new Queries(null, fallback);
        require(PendaftaranWisudaMahasiswaAction.ambilSkripsiUntukWisuda(q.session(), mahasiswa)
                == fallback, "Ungraded thesis must remain editable");
        require(q.queries == 2, "Expected fallback query");
        for (int i = 1; i <= 2; i++) {
            require(q.calls.contains(i + ":addOrder:semester desc"), "Semester order missing");
            require(q.calls.contains(i + ":addOrder:id desc"), "Deterministic tie-break missing");
            require(q.calls.contains(i + ":setMaxResults:1"), "Query must be bounded");
            require(q.calls.get((i - 1) * 5).startsWith(i + ":add:mahasiswa="),
                    "Student filter missing");
        }
        require(!q.calls.contains("2:add:totalNilai>0.1"), "Fallback must include ungraded thesis");
        q = new Queries(null, null);
        require(PendaftaranWisudaMahasiswaAction.ambilSkripsiUntukWisuda(q.session(), mahasiswa)
                == null, "Missing thesis must stay missing");
        q = new Queries(graded, fallback);
        require(PendaftaranWisudaMahasiswaAction.ambilSkripsiUntukWisuda(q.session(), null)
                == null && q.queries == 0, "Missing student must not query other students");
        System.out.println("SkripsiWisudaSelectionSelfTest: PASS");
    }
}
