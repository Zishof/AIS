package ais.ui.util;

import java.util.List;

import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zk.ui.Component;

import ais.common.CommonDashboardHtmlHelper;

/**
 * Helper ringan untuk dasbor yang sebelumnya langsung membuat file Excel.
 * Data dapat ditampilkan lebih dulu sebagai tabel ZK 5.5, lalu tombol download tetap memakai format asli.
 */
public class DashboardExcelPreviewUtil {

    private static final int DEFAULT_MAX_PREVIEW_ROWS = 500;

    public static Html info(String title, String description) {
        return new Html(CommonDashboardHtmlHelper.panel(title, description, null));
    }

    public static Grid createPreviewGrid(Component parent, String[] headers, List rows, int pageSize) {
        return createPreviewGrid(parent, headers, rows, pageSize, DEFAULT_MAX_PREVIEW_ROWS);
    }

    public static Grid createPreviewGrid(Component parent, String[] headers, List rows, int pageSize, int maxPreviewRows) {
        Grid grid = new Grid();
        grid.setMold("paging");
        grid.setPageSize(pageSize <= 0 ? 20 : pageSize);
        grid.setWidth("100%");
        grid.setSclass("dgrid fgrid");
        grid.setParent(parent);

        Columns cols = new Columns();
        cols.setParent(grid);
        if (headers != null) {
            for (int i = 0; i < headers.length; i++) {
                MyColumnConfig c = new MyColumnConfig(headers[i]);
                c.setParent(cols);
            }
        }

        Rows zkRows = new Rows();
        zkRows.setParent(grid);
        if (rows != null) {
            int batas = maxPreviewRows <= 0 ? rows.size() : Math.min(rows.size(), maxPreviewRows);
            for (int i = 0; i < batas; i++) {
                Object data = rows.get(i);
                Row row = new Row();
                row.setValign("top");
                row.setParent(zkRows);
                if (data instanceof Object[]) {
                    Object[] arr = (Object[]) data;
                    for (int j = 0; j < arr.length; j++) {
                        row.appendChild(new Label(arr[j] == null ? "" : String.valueOf(arr[j])));
                    }
                } else {
                    row.appendChild(new Label(data == null ? "" : String.valueOf(data)));
                }
            }
            if (maxPreviewRows > 0 && rows.size() > maxPreviewRows) {
                Row row = new Row();
                row.setValign("top");
                row.setParent(zkRows);
                row.appendChild(new Label("Preview menampilkan " + maxPreviewRows
                        + " baris pertama. Gunakan tombol download untuk mengambil file lengkap."));
            }
        }
        return grid;
    }

    public static String html(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
