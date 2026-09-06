package ais.common.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import ais.common.Common;
import ais.common.CommonExcelContentHelper;
import ais.database.model.Pertemuan;

/** Regresi murni in-memory; tidak membuka koneksi database. */
public final class EcampusSeptemberRegressionSelfTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Pertemuan pertemuan = new Pertemuan();
        pertemuan.setAbsensi(";1,;2;3, 4 ;5,6;");
        check(pertemuan.retreiveAbsensiId(1L).equals(-1L), "Empty status");
        check(pertemuan.retreiveAbsensiId(2L).equals(-1L), "Incomplete row");
        check(pertemuan.retreiveAbsensiId(3L).equals(4L), "Whitespace status");
        check(pertemuan.retreiveAbsensiId(5L).equals(6L), "Valid status");
        check(pertemuan.retreiveAbsensiId(null).equals(-1L), "Missing participant");

        XSSFWorkbook book = new XSSFWorkbook();
        XSSFSheet sheet = book.createSheet("Test");
        XSSFCell text = sheet.createRow(0).createCell(0);
        text.setCellValue("Jawaban esai");
        XSSFCellStyle dateStyle = book.createCellStyle();
        dateStyle.setDataFormat(book.createDataFormat().getFormat("dd-mm-yyyy"));
        text.setCellStyle(dateStyle);
        check("Jawaban esai".equals(Common.getCellContent(text)), "Text in date-formatted cell");
        sheet.createRow(1).createCell(0).setCellValue("");
        check(CommonExcelContentHelper.getSheetContentAsDate(sheet, 0, 1) == null,
                "Blank date must remain absent");

        final List<String> calls = new ArrayList<String>();
        Session session = (Session) Proxy.newProxyInstance(Session.class.getClassLoader(),
                new Class<?>[] { Session.class }, new InvocationHandler() {
                    boolean open = true;
                    public Object invoke(Object proxy, Method method, Object[] values) {
                        String name = method.getName();
                        if ("isOpen".equals(name)) return Boolean.valueOf(open);
                        calls.add(name);
                        if ("close".equals(name)) open = false;
                        return null;
                    }
                });
        Common.closeNativeSessionQuietly(session);
        Common.closeNativeSessionQuietly(session);
        check(calls.size() == 3 && "clear".equals(calls.get(0))
                && "disconnect".equals(calls.get(1)) && "close".equals(calls.get(2)),
                "Owned session cleanup must run once: " + calls);
        System.out.println("PASS EcampusSeptemberRegressionSelfTest");
    }
}
