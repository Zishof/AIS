package ais.action.master.sapto;

import java.io.ByteArrayOutputStream;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.model.Jurusan;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Base class for all SAPTO laporan windows.
 * Provides standardized layout: North toolbar + Center content area.
 * Subclasses implement buildFilters(), getSheetCode(), and onCetak().
 */
public abstract class SaptoBaseWindow extends MyWindow {

    private static final long serialVersionUID = 1L;
    private static final String MIME_XLSX =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    protected Center contentCenter;

    /** Shared Fakultas + Jurusan combobox — subclass dapat langsung pakai tanpa deklarasi ulang. */
    protected Combobox fakultas = new Combobox();
    protected Combobox jurusan  = new Combobox();

    /**
     * Inisialisasi Fakultas dan Jurusan dengan relasi dua arah.
     * Menggantikan Common.initJurusanDanSemua(jurusan, ConstantValues.s1) di setiap laporan.
     */
    protected void initFakultasJurusan() {
        Common.initFakultasDanJurusanDanSemua(fakultas, jurusan);
    }

    /**
     * Tambahkan label + combobox Fakultas dan Jurusan ke row filter.
     * Auto-trigger onCetak saat salah satu berubah.
     */
    protected void addFakultasJurusanFilter(final Row row) {
        row.appendChild(new MyLabelConfig("Fakultas"));
        fakultas.setWidth("90%");
        row.appendChild(fakultas);
        fakultas.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });

        row.appendChild(new MyLabelConfig("Program Studi"));
        jurusan.setWidth("90%");
        row.appendChild(jurusan);
        jurusan.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });
    }

    /** Kembalikan Jurusan yang dipilih, atau null jika belum dipilih. */
    protected Jurusan getSelectedJurusan() {
        if (jurusan == null || jurusan.getSelectedItem() == null) return null;
        Object val = jurusan.getSelectedItem().getValue();
        return (val instanceof Jurusan) ? (Jurusan) val : null;
    }

    public SaptoBaseWindow() {
        super();
    }

    public SaptoBaseWindow(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
    }

    protected void buildBase(boolean autoLoad) {
        buildBase(autoLoad, true);
    }

    protected void buildBase(boolean autoLoad, boolean showNorth) {
        setHeight("100%");
        setWidth("100%");

        Borderlayout bl = new MyBorderlayout();
        bl.setParent(this);

        North north = new North();
        north.setCollapsible(true);
        north.setVisible(showNorth);
        north.setParent(bl);
        ZkCompat.setFlex(north, true);

        MyGrid toolGrid = new MyGrid();
        toolGrid.setWidth("100%");
        toolGrid.setParent(north);

        contentCenter = new Center();
        contentCenter.setParent(bl);
        ZkCompat.setFlex(contentCenter, true);

        Rows rows = new Rows();
        rows.setParent(toolGrid);

        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);

        buildFilters(row);

        MyToolbarbuttonConfig btnSearch = new MyToolbarbuttonConfig("Tampilkan Data", "/img/svg/search.svg");
        btnSearch.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onCetak(null);
            }
        });
        btnSearch.setParent(row);

        MyToolbarbuttonConfig btnDownload = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
        btnDownload.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                try {
                    Spreadsheet excelku = (Spreadsheet) contentCenter.getAttribute("excelku");
                    if (excelku == null) {
                        Common.showInfo("Tampilkan data terlebih dahulu, lalu klik Download.");
                        return;
                    }
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    excelku.getBook().write(bout);
                    bout.close();
                    Filedownload.save(bout.toByteArray(), MIME_XLSX, getSheetCode() + ".xlsx");
                } catch (Exception ex) {
                    Common.tampilErrorJikaAdmin(ex);
                }
            }
        });
        btnDownload.setParent(row);

        if (autoLoad) {
            onCetak(null);
        }
    }

    protected void buildFilters(Row row) {
        // Override in subclass to add filter widgets
    }

    protected abstract String getSheetCode();

    public abstract void onCetak(Event event);

    protected void clearContent() {
        Common.clear(contentCenter);
    }

    protected void display(org.zkoss.zul.Label label, int maxCols) {
        SaptoUtil.displayWorksheet(label, getSheetCode(), contentCenter, maxCols);
    }

    protected void display(org.zkoss.zul.Label label, int maxCols,
                           EventListener onCellClick) {
        SaptoUtil.displayWorksheet(label, getSheetCode(), contentCenter, maxCols, onCellClick);
    }
}
