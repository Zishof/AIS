package ais.action.master.spmi;

import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Base class shared by all SPMI action controllers.
 *
 * Provides: security check, privilege flags, paging wiring,
 * cetak/upload toolbar buttons, form popup building helpers,
 * and grid refresh.  Subclasses only need to provide the
 * entity-specific parts (search criteria, form rows, save logic).
 */
public abstract class BaseSPMIAction extends GenericAutowireComposer
        implements DataCriteria, DataSearchDefault, DataInitDefault {

    private static final long serialVersionUID = 1L;

    // ---- ZUL-autowired fields (IDs must match in every ZUL that uses this) ----
    protected MyWindow         addWindow;
    protected Paging           paging;
    protected MyGrid           grid;
    protected Textbox          searchnama;
    protected Checkbox         searchaktif;
    protected MyToolbarbuttonConfig add;

    // ---- Privilege flags ----
    protected boolean edit;
    protected boolean delete;

    // ---- Holder returned by prepareFormWindow ----
    protected static final class FormHolder {
        public final Borderlayout borderlayout;
        public final Rows         rows;

        FormHolder(Borderlayout bl, Rows r) {
            this.borderlayout = bl;
            this.rows         = r;
        }
    }

    // =====================================================================
    // ZK lifecycle
    // =====================================================================

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
            org.zkoss.zk.ui.Page page,
            org.zkoss.zk.ui.Component parent,
            org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    // =====================================================================
    // Shared initialisation helpers
    // =====================================================================

    /** Reads privilege flags from the session and configures the Add button. */
    protected void initPrivileges() {
        if (add != null) {
        add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
        add.setTooltiptext("Tambah");
        }
        edit   = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
    }

    /** Wires the paging component so that page changes trigger onSearchDefault. */
    protected void initPagingListener() {
        Common.initPaging(paging, new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onSearchDefault(null);
            }
        });
    }

    /**
     * Appends Cetak and Upload toolbar buttons after the Add button.
     * Upload is only shown when the user has full CRUD rights.
     */
    protected void appendCetakUpload(Class<?> clazz, String[] contents) {
        MyToolbarbuttonConfig cetak = Common.cetakData(clazz, this, contents);
        if (add != null) {
        add.getParent().appendChild(cetak);
        }

        MyToolbarbuttonConfig upload = Common.uploadData(this, clazz, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    // =====================================================================
    // Add/Edit popup helpers
    // =====================================================================

    /** Makes the addWindow visible and opens it as a modal dialog. */
    protected void openAddWindow() throws Exception {
        addWindow.setVisible(true);
        addWindow.onModal();
    }

    /**
     * Clears and sets the title of the popup window, then builds the
     * standard two-column BorderLayout + Grid container.
     *
     * @return a FormHolder with the Borderlayout and the Rows component
     *         into which the caller should append form rows.
     */
    protected FormHolder prepareFormWindow(String title) {
        addWindow.setTitle(title);
        Common.clear(addWindow);

        Borderlayout bl = new MyBorderlayout();

        Center center = new Center();
        center.setParent(bl);
        ZkCompat.setFlex(center, true);

        MyGrid formGrid = new MyGrid();
        formGrid.setWidth("100%");
        formGrid.setHeight("100%");
        formGrid.setParent(center);

        Columns columns = new Columns();
        columns.setParent(formGrid);
        MyColumnConfig labelCol = new MyColumnConfig();
        labelCol.setWidth("30%");
        labelCol.setParent(columns);
        new MyColumnConfig().setParent(columns);

        Rows rows = new Rows();
        rows.setParent(formGrid);

        return new FormHolder(bl, rows);
    }

    /**
     * Adds the South toolbar (Batal + Simpan) and attaches the border
     * layout to addWindow.  Must be called after all form rows are added.
     */
    protected void finaliseFormWindow(final FormHolder fh, final EventListener onSaveAction) {
        South south = new South();
        ZkCompat.setFlex(south, true);
        south.setParent(fh.borderlayout);

        Toolbar toolbar = new Toolbar();
        toolbar.setParent(south);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.setTooltiptext("Simpan");
        save.addEventListener("onClick", onSaveAction);
        save.setParent(toolbar);

        fh.borderlayout.setParent(addWindow);
    }

    // =====================================================================
    // Form row builder
    // =====================================================================

    /**
     * Appends a labelled form row to {@code rows} and returns it so the
     * caller can add the input component.
     */
    protected Row addFormRow(Rows rows, String labelText) {
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new MyLabelConfig(labelText));
        return row;
    }

    // =====================================================================
    // Grid refresh helper
    // =====================================================================

    @SuppressWarnings("unchecked")
    protected <T> void refreshGridData(List<T> data, MyRowRenderer renderer) {
        grid.setRowRenderer(renderer);
        grid.setModelCheckMobile(new SimpleListModel(data));
    }

    /** Null-safe helper to read the selected value from a Combobox. */
    @SuppressWarnings("unchecked")
    protected static <T> T selectedValue(Combobox cb) {
        return (cb == null || cb.getSelectedItem() == null) ? null : (T) cb.getSelectedItem().getValue();
    }
}
