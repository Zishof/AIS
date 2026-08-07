package ais.action.master.generic.v2;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** XLSX streaming sink untuk export sinkron berukuran terbatas. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenericCrudExportService {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();
    private final GenericCrudQueryService query = new GenericCrudQueryService();

    public void writeXlsx(GenericCrudRequestContext context, String search, HttpServletResponse response) throws Exception {
        privilege.require(context, GenericCrudOperation.EXPORT);
        List fields = exportFields(context.getDefinition());
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            XSSFSheet sheet = workbook.createSheet(safeSheet(context.getDefinition().getDisplayName()));
            Row header = sheet.createRow(0);
            for (int i = 0; i < fields.size(); i++) {
                GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
                header.createCell(i).setCellValue(field.getLabel());
            }
            int rowIndex = 1;
            int page = 1;
            int limit = 5000;
            while (rowIndex <= limit) {
                GenericCrudPage data = query.list(context, page, context.getDefinition().getMaxPageSize(), search, new ArrayList(), null);
                List rows = data.getRows();
                for (int i = 0; i < rows.size() && rowIndex <= limit; i++) {
                    Row output = sheet.createRow(rowIndex++);
                    Map source = (Map) rows.get(i);
                    for (int c = 0; c < fields.size(); c++) {
                        GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(c);
                        Cell cell = output.createCell(c);
                        Object value = source.get(field.getProperty());
                        if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue());
                        else if (value instanceof Boolean) cell.setCellValue(((Boolean) value).booleanValue());
                        else cell.setCellValue(value == null ? "" : String.valueOf(value));
                    }
                }
                if (rows.isEmpty() || rowIndex > data.getTotal() || rows.size() < data.getPageSize()) break;
                page++;
            }
            for (int i = 0; i < fields.size(); i++) sheet.autoSizeColumn(i);
            String file = URLEncoder.encode(context.getDefinition().getPageKey() + ".xlsx", "UTF-8").replace("+", "%20");
            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + file);
            response.setHeader("X-Content-Type-Options", "nosniff");
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        } finally { try { workbook.close(); } catch (Exception ignored) { } }
    }

    private List exportFields(GenericCrudDefinition definition) {
        List result = new ArrayList();
        List fields = definition.getFields();
        for (int i = 0; i < fields.size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
            if (field.isExportable() && !field.isSensitive()) result.add(field);
        }
        return result;
    }
    private String safeSheet(String value) { String result = value == null ? "Data" : value.replaceAll("[\\\\/?*\\[\\]:]", "_"); return result.length() > 31 ? result.substring(0, 31) : result; }
}
