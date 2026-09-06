package ais.common.test;

import java.lang.reflect.Method;
import org.json.JSONObject;
import ais.action.report.Report;
import ais.database.model.DetailBiaya;
import ais.database.model.Kegiatan;

/** Pengujian offline: diagnostik terbatas dan snapshot nominal tetap numerik. */
public final class Ecampus58RegressionSelfTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Method format = Report.class.getDeclaredMethod("diagnosticValue", Object.class);
        format.setAccessible(true);
        Object expensive = new Object() {
            public String toString() { throw new AssertionError("Must not expand entities/collections"); }
        };
        check(((String) format.invoke(null, expensive)).startsWith("["), "Complex value summarized");
        String large = new String(new char[200000]).replace('\0', 'x');
        check(((String) format.invoke(null, large)).length() < 1100, "Large parameter bounded");
        check("123".equals(format.invoke(null, Long.valueOf(123))), "Scalar retained");

        Exception a = new Exception(large);
        Exception b = new Exception("cause", a);
        a.initCause(b);
        Method stack = Report.class.getDeclaredMethod("stackTraceToString", Throwable.class);
        stack.setAccessible(true);
        String trace = (String) stack.invoke(null, a);
        check(trace.length() < 62000 && trace.contains("cause"), "Cyclic cause terminates, stack bounded");
        Method root = Report.class.getDeclaredMethod("getRootCause", Throwable.class);
        root.setAccessible(true);
        check(root.invoke(null, a) != null, "Cyclic root lookup terminates");

        org.zkoss.zul.Iframe frame = new org.zkoss.zul.Iframe();
        Method preview = Report.class.getDeclaredMethod("tampilkanPratinjauPdfTidakTersedia", org.zkoss.zk.ui.Component.class);
        preview.setAccessible(true);
        preview.invoke(null, frame);
        check(frame.getChildren().isEmpty() && frame.getContent() != null,
                "Iframe error uses media, never child components");

        Kegiatan kegiatan = new Kegiatan();
        DetailBiaya biaya = new DetailBiaya();
        biaya.setId(42L);
        kegiatan.simpanNominalTagihanTerkunci(biaya, null, null, 10.0, 12.0, "Koreksi uji", "uji", "Uji");
        JSONObject record = new JSONObject(kegiatan.getNominalTagihanKunciJson())
                .getJSONArray("riwayat").getJSONObject(0);
        check(record.get("waktuEpoch") instanceof Number, "Timestamp remains JSON number");
        check(record.getLong("waktuEpoch") > 0, "Timestamp retained");
        check(record.getDouble("nominal") == 12.0 && record.getDouble("nominalSebelum") == 10.0,
                "Nominal and audit retained");
        System.out.println("PASS Ecampus58RegressionSelfTest");
    }
}
