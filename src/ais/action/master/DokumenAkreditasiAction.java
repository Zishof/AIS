package ais.action.master;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.DokumenAkreditasiTreeModel;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Akreditasi;
import ais.database.model.DokumenAkreditasi;
import ais.database.model.DspaceInformation;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DokumenAkreditasiAction extends Groupbox implements DataCriteria, DataSearchDefault, DataInitDefault {

    private static final long serialVersionUID = -5779730267402400328L;

    private MyWindow addWindow;
    private Paging paging;
    private MyGrid grid;
    private Textbox searchnama;
    private Textbox nama;
    private Textbox keterangan;
    private Textbox kode;
    private MyDatebox tanggalDokumen;
    private Intbox nomorUrut;
    private AmbilDataSatuanKerjaBanbox satuanKerja;
    private AmbilDataSatuanKerjaBanbox searchparent;
    private Akreditasi akreditasi;
    private DokumenAkreditasi indukDokumenAkreditasi;
    private DokumenAkreditasi dokumenAkreditasi;
    private DokumenAkreditasi induk;
    private LampiranLain lainMahasiswa;
    private MyToolbarbuttonConfig add;
    private boolean create;
    private boolean edit;
    private boolean delete;
    private boolean simple;
    private SatuanKerjaTreeModel satuanKerjaTreeModel;
    private Tree tree;
    private DokumenAkreditasiTreeModel dokumenAkreditasiTreeModel;
    private MyTabConfig tabExplorer;
    private AbstractComponent tabList;

    public DokumenAkreditasiAction(Akreditasi akreditasi, DokumenAkreditasi indukDokumenAkreditasi, boolean simple,
            AmbilDataSatuanKerjaBanbox searchparent) {
        super();
        this.akreditasi = akreditasi;
        this.indukDokumenAkreditasi = indukDokumenAkreditasi;
        this.simple = simple;
        this.searchparent = searchparent;
    }

    public void init() {
        satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
        create = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
        edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
        Common.clear(this);
        setMold("3d");
        setWidth("100%");
        setStyle("border:0; background:#f8fafc; padding:0; margin:0;");
        buildToolbar();
        buildExplorer();
        onSearchDefault(null);
    }

    private void buildToolbar() {
        Toolbar toolbar = new Toolbar();
        toolbar.setVisible(!simple);
        toolbar.setParent(this);
        toolbar.setStyle("background:#ffffff; border:1px solid #e5e7eb; border-radius:12px; padding:8px; margin:8px;");
        add = new MyToolbarbuttonConfig("Tambah Ruang/File", "/img/svg/addthis.svg");
        add.setVisible(create);
        add.setTooltiptext("Tambah ruang arsip, sub ruang, atau file dokumen");
        add.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                onAdd(event);
            }
        });
        toolbar.appendChild(add);

        String[] contents = new String[] { "id", "kode", "nama", "tanggalDokumen", "aktif", "keterangan",
                "nomorUrut", "induk", "akreditasi", "satuanKerja" };
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(DokumenAkreditasi.class, this, contents);
        toolbar.appendChild(cetakToolbarbutton);
        MyToolbarbuttonConfig upload = Common.uploadData(this, DokumenAkreditasi.class, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        toolbar.appendChild(upload);

        toolbar.appendChild(new Space());
        searchnama = new Textbox();
        searchnama.setCols(24);
        searchnama.setTooltiptext("Cari nama, kode, atau keterangan dokumen");
        searchnama.addEventListener("onOK", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                refreshAll();
            }
        });
        toolbar.appendChild(searchnama);
        MyToolbarbuttonConfig find = new MyToolbarbuttonConfig("Cari", "/img/search.png");
        find.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                refreshAll();
            }
        });
        toolbar.appendChild(find);
        toolbar.appendChild(new Space());
        initDspaceButtons(toolbar);
    }

    private void initDspaceButtons(Toolbar toolbar) {
        boolean visibleDspace = Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
                && Common.bolehKonfigurasi("dokumen_terhubung_ke_dspace");
        MyToolbarbuttonConfig export = new MyToolbarbuttonConfig("Ekspor DSpace", "/img/corner.gif");
        export.setVisible(visibleDspace);
        export.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                prosesDspaceExport(true);
            }
        });
        toolbar.appendChild(export);
        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batalkan Ekspor", "/img/svg/trash.svg");
        cancel.setVisible(visibleDspace);
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                prosesDspaceDelete();
            }
        });
        toolbar.appendChild(cancel);
    }

    private void buildExplorer() {
        Tabbox tabbox = new Tabbox();
        tabbox.setParent(this);
        tabbox.setWidth("100%");
        tabbox.setHeight("100%");
        Tabs tabs = new Tabs();
        tabs.setParent(tabbox);
        tabExplorer = new MyTabConfig("Ruang Arsip", "/img/svg/folder.svg");
        tabExplorer.setParent(tabs);
        tabList = new MyTabConfig("Daftar Dokumen", "/img/svg/list.svg");
        tabList.setParent(tabs);
        Tabpanels tabpanels = new Tabpanels();
        tabpanels.setParent(tabbox);
        Tabpanel panelExplorer = new ais.ui.util.MyTabpanel();
        panelExplorer.setStyle("padding:8px; overflow:auto;");
        panelExplorer.setParent(tabpanels);
        Tabpanel panelList = new ais.ui.util.MyTabpanel();
        panelList.setStyle("padding:8px; overflow:auto;");
        panelList.setParent(tabpanels);
        buildTree(panelExplorer);
        buildList(panelList);
    }

    private void buildTree(Tabpanel parent) {
        tree = new Tree();
        tree.setZclass("z-dottree");
        tree.setWidth("100%");
        tree.setHeight("550px");
        tree.setParent(parent);
        tree.setStyle("background:#ffffff; border:1px solid #e5e7eb; border-radius:14px; overflow:auto;");
        Treecols columns = new Treecols();
        columns.setParent(tree);
        Treecol column = new Treecol("Nama Ruang/File");
        column.setParent(columns);
        column = new Treecol("Tanggal");
        column.setWidth("120px");
        column.setParent(columns);
        column = new Treecol("Keterangan");
        column.setWidth("25%");
        column.setParent(columns);
        column = new Treecol("Aktif");
        column.setWidth("85px");
        column.setParent(columns);
        column = new Treecol("");
        column.setWidth("150px");
        column.setParent(columns);
        dokumenAkreditasiTreeModel = new DokumenAkreditasiTreeModel(akreditasi, searchparent);
        tree.setModel(dokumenAkreditasiTreeModel);
        tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {
            @Override
            public void render(final Treeitem treeitem, Object value) throws Exception {
                renderTreeItem(treeitem, (DokumenAkreditasi) value);
            }
        });
    }

    private void buildList(Tabpanel parent) {
        grid = new MyGrid();
        grid.setSclass("fgrid");
        grid.setWidth("100%");
        grid.setParent(parent);
        Columns columns = new Columns();
        columns.setParent(grid);
        Column column = new Column("Dokumen");
        column.setParent(columns);
        column = new Column("Induk");
        column.setWidth("18%");
        column.setParent(columns);
        if (!simple) {
            column = new Column("Tanggal");
            column.setWidth("110px");
            column.setParent(columns);
            column = new Column("Aktif");
            column.setWidth("80px");
            column.setParent(columns);
            column = new Column("");
            column.setWidth("120px");
            column.setParent(columns);
        }
        paging = new Paging();
        paging.setDetailed(true);
        paging.setParent(parent);
        Common.initPaging(paging, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                onSearchDefault(null);
            }
        });
    }

    private void renderTreeItem(final Treeitem treeitem, final DokumenAkreditasi dokumen) throws Exception {
        Treerow treerow = new Treerow();
        treerow.setParent(treeitem);
        Treecell cell = new Treecell();
        cell.setParent(treerow);
        renderDocumentTitle(cell, dokumen, true);
        cell = new Treecell();
        cell.setParent(treerow);
        cell.appendChild(new Label(formatDate(dokumen.getTanggalDokumen())));
        cell = new Treecell();
        cell.setParent(treerow);
        cell.appendChild(new MyLabelAgakKecil(dokumen.getKeterangan()));
        cell = new Treecell();
        cell.setParent(treerow);
        final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
        checkbox.setDisabled(!edit);
        checkbox.setChecked(dokumen.getAktif());
        checkbox.setParent(cell);
        checkbox.addEventListener("onCheck", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                dokumen.setAktif(checkbox.isChecked());
                Common.refreshSaveOrUpdate(dokumen);
            }
        });
        cell = new Treecell();
        cell.setParent(treerow);
        Hbox tools = new Hbox();
        tools.setParent(cell);
        appendTreeButton(tools, treeitem, dokumen, "", "/img/svg/addthis.svg", "Tambah Child", "add");
        appendTreeButton(tools, treeitem, dokumen, "", "/img/svg/edit-copy.svg", "Copy", "copy");
        appendTreeButton(tools, treeitem, dokumen, "", "/img/svg/edit-box-line.svg", "Ubah", "edit");
        if (delete && dokumenAkreditasiTreeModel.getChildCount(dokumen) == 0) {
            appendTreeButton(tools, treeitem, dokumen, "", "/img/svg/trash.svg", "Hapus", "delete");
        }
    }

    private void renderDocumentTitle(Component parent, final DokumenAkreditasi dokumen, boolean treeMode) {
        LampiranLain lampiran = LampiranLain.ambil(dokumen.getId(), DokumenAkreditasi.class.getName());
        boolean hasFile = lampiran != null;
        Hbox titleBox = new Hbox();
        titleBox.setAlign("center");
        titleBox.setSpacing("6px");
        titleBox.setParent(parent);
        titleBox.appendChild(new Html(buildDocumentIconHtml(hasFile)));
        String text = buildDocumentTitleText(dokumen, hasFile);
        if (hasFile) {
            A link = new A(text);
            link.setStyle("font-weight:700; color:#1d4ed8; text-decoration:none;");
            link.setTooltiptext("Unduh lampiran dokumen");
            link.setParent(titleBox);
            link.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    downloadDokumen(dokumen);
                }
            });
        } else if (treeMode) {
            Label label = new Label(text);
            label.setStyle("font-weight:700; color:#0f172a;");
            label.setParent(titleBox);
        } else {
            Vbox revisiBox = RevisiHelper.createNewRevisi(DokumenAkreditasi.class, dokumen, text);
            revisiBox.setParent(titleBox);
        }
        if (!treeMode) {
            Vbox metaBox = new Vbox();
            metaBox.setParent(parent);
            if (!empty(dokumen.getKeterangan())) {
                new MyLabelAgakKecil(dokumen.getKeterangan()).setParent(metaBox);
            }
            renderUploadBox(metaBox, dokumen, false);
        }
    }

    private String buildDocumentTitleText(DokumenAkreditasi dokumen, boolean hasFile) {
        StringBuilder sb = new StringBuilder();
        sb.append(hasFile ? "File Dokumen" : "Ruang Arsip");
        sb.append(" - ");
        if (dokumen != null && !empty(dokumen.getKode())) {
            sb.append(dokumen.getKode()).append(" - ");
        }
        sb.append(dokumen == null ? "" : dokumen.getNama());
        return sb.toString();
    }

    private String buildDocumentIconHtml(boolean hasFile) {
        if (hasFile) {
            return "<span style='display:inline-flex; width:30px; height:30px; align-items:center; justify-content:center; border-radius:10px; background:#dcfce7; color:#15803d;'><i class='fa fa-file-text-o'></i></span>";
        }
        return "<span style='display:inline-flex; width:30px; height:30px; align-items:center; justify-content:center; border-radius:10px; background:#eff6ff; color:#1d4ed8;'><i class='fa fa-folder-open'></i></span>";
    }

    private void renderUploadBox(Component parent, DokumenAkreditasi dokumen, boolean captureNewUpload) {
        Hbox box = new Hbox();
        LampiranLain.createDownloadUploadFileLain(box, dokumen.getId(), DokumenAkreditasi.class.getName(), "Dokumen",
                false, new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                    }
                }, null, false, false, false, edit && delete);
        box.setParent(parent);
    }

    private void appendTreeButton(Hbox tools, final Treeitem treeitem, final DokumenAkreditasi dokumen, String label,
            String image, String tooltip, final String action) {
        MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(label, image);
        button.setTooltiptext(tooltip);
        if ("add".equals(action)) {
            button.setVisible(create);
        } else if ("delete".equals(action)) {
            button.setVisible(delete);
        } else {
            button.setVisible(edit);
        }
        button.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if ("delete".equals(action)) {
                    confirmDelete(dokumen, treeitem);
                } else if ("add".equals(action)) {
                    openEditor(new DokumenAkreditasi(akreditasi, dokumen), dokumen, new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            reloadTreeitem(treeitem);
                        }
                    });
                } else if ("copy".equals(action)) {
                    DokumenAkreditasi copy = new DokumenAkreditasi(akreditasi, dokumen.getInduk());
                    copy.setKode(dokumen.getKode());
                    copy.setNama(dokumen.getNama());
                    copy.setKeterangan(dokumen.getKeterangan());
                    copy.setNomorUrut(dokumen.getNomorUrut());
                    openEditor(copy, dokumen.getInduk(), new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            reloadTreeitem(treeitem);
                        }
                    });
                } else {
                    openEditor(dokumen, dokumen.getInduk(), new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            reloadTreeitem(treeitem);
                        }
                    });
                }
            }
        });
        button.setParent(tools);
    }

    private void reloadTreeitem(final Treeitem treeitem) {
        refreshTreeModel();
        final Treeitem parent = treeitem == null ? null : treeitem.getParentItem();
        if (parent == null) {
            onSearchDefault(null);
            return;
        }
        parent.unload();
        final Timer timer = new Timer(200);
        timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        timer.addEventListener("onTimer", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                parent.setOpen(true);
                timer.detach();
            }
        });
        timer.start();
    }

    class DokumenAkreditasiRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object value) throws Exception {
            row.setValign("top");
            final DokumenAkreditasi dokumen = (DokumenAkreditasi) value;
            Vbox docBox = new Vbox();
            docBox.setParent(row);
            renderDocumentTitle(docBox, dokumen, false);
            new Label(dokumen.getInduk() == null ? "Root" : dokumen.getInduk().getNama()).setParent(row);
            if (!simple) {
                new Label(formatDate(dokumen.getTanggalDokumen())).setParent(row);
                final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
                checkbox.setDisabled(!edit);
                checkbox.setChecked(dokumen.getAktif());
                checkbox.setParent(row);
                checkbox.addEventListener("onCheck", new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        dokumen.setAktif(checkbox.isChecked());
                        Common.refreshSaveOrUpdate(dokumen);
                    }
                });
                Common.copyEditDeleteButtons(edit, delete, dokumen, DokumenAkreditasiAction.this).setParent(row);
            }
        }
    }

    public void onAdd(Event event) throws Exception {
        openEditor(new DokumenAkreditasi(akreditasi, indukDokumenAkreditasi), indukDokumenAkreditasi, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                refreshAll();
            }
        });
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        DokumenAkreditasi data = (DokumenAkreditasi) obj;
        openEditor(data, data.getInduk(), new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                refreshAll();
            }
        });
    }

    private void openEditor(DokumenAkreditasi data, DokumenAkreditasi parent, EventListener listener) throws Exception {
        addWindow = new MyWindow(data.getId() == null ? "Tambah Ruang/File" : "Ubah Ruang/File", "none", true);
        addWindow.setHeight("95%");
        addWindow.setWidth(Common.isMobile() ? "98%" : "640px");
        addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        initForm(data, parent, listener);
        addWindow.setVisible(true);
        addWindow.onModal();
    }

    private void initForm(final DokumenAkreditasi data, DokumenAkreditasi parent, final EventListener listener) throws Exception {
        dokumenAkreditasi = data;
        induk = parent;
        Common.clear(addWindow);
        Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
        Center center = new Center();
        center.setParent(borderlayout);
        ais.ui.util.ZkCompat.setFlex(center, true);
        MyGrid form = new MyGrid();
        form.setWidth("100%");
        form.setHeight("100%");
        form.setParent(center);
        Columns columns = new Columns();
        columns.setParent(form);
        MyColumnConfig column = new MyColumnConfig();
        column.setWidth("30%");
        column.setParent(columns);
        column = new MyColumnConfig();
        column.setParent(columns);
        Rows rows = new Rows();
        rows.setParent(form);
        MyFormRow hero = new MyFormRow();
        ais.ui.util.ZkCompat.setSpans(hero, "2");
        hero.setParent(rows);
        hero.appendChild(new Html(buildFormHeroHtml(parent)));
        kode = createTextbox(rows, "Kode", data.getKode(), 1);
        nama = createTextbox(rows, "Nama Ruang/File *", data.getNama(), 1);
        initSatuanKerja(rows, data);
        tanggalDokumen = createDatebox(rows, "Tanggal Dokumen", data.getTanggalDokumen());
        keterangan = createTextbox(rows, "Keterangan", data.getKeterangan(), 4);
        nomorUrut = createIntbox(rows, "Nomor Urut *", data.getNomorUrut());
        MyFormRow uploadRow = new MyFormRow();
        uploadRow.setValign("top");
        uploadRow.setParent(rows);
        uploadRow.appendChild(new ais.ui.util.MyLabelConfig("Lampiran"));
        Hbox hbox = new Hbox();
        LampiranLain.createDownloadUploadFileLain(hbox, data.getId(), DokumenAkreditasi.class.getName(), "Dokumen", false,
                new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        lainMahasiswa = (LampiranLain) event.getData();
                    }
                });
        hbox.setParent(uploadRow);
        MyFormRow note = new MyFormRow();
        ais.ui.util.ZkCompat.setSpans(note, "2");
        note.setParent(rows);
        note.appendChild(new Html("<div style='padding:8px 10px; background:#f8fafc; border:1px solid #e5e7eb; border-radius:10px; color:#475569; font-size:11px;'>Gunakan item ini sebagai ruang arsip jika belum memiliki lampiran. Jika lampiran lebih dari satu, kompres menjadi ZIP sebelum diunggah.</div>"));
        South south = new South();
        ais.ui.util.ZkCompat.setFlex(south, true);
        south.setParent(borderlayout);
        Toolbar toolbar = new Toolbar();
        toolbar.setParent(south);
        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);
        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    addWindow.setVisible(false);
                    if (listener != null) {
                        listener.onEvent(event);
                    }
                }
            }
        });
        save.setParent(toolbar);
        borderlayout.setParent(addWindow);
    }

    private Textbox createTextbox(Rows rows, String label, String value, int rowsCount) {
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig(label));
        Textbox textbox = new Textbox(value == null ? "" : value);
        textbox.setWidth("90%");
        textbox.setRows(rowsCount);
        row.appendChild(textbox);
        return textbox;
    }

    private Intbox createIntbox(Rows rows, String label, Integer value) {
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig(label));
        Intbox intbox = new Intbox(value);
        intbox.setWidth("90%");
        row.appendChild(intbox);
        return intbox;
    }

    private MyDatebox createDatebox(Rows rows, String label, java.util.Date value) {
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig(label));
        MyDatebox datebox = new MyDatebox(value);
        datebox.setFormat(Common.dateFormat.get().toPattern());
        datebox.setReadonly(true);
        datebox.setWidth("90%");
        row.appendChild(datebox);
        return datebox;
    }

    private void initSatuanKerja(Rows rows, DokumenAkreditasi data) throws Exception {
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
        satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
        satuanKerja.setWidth("90%");
        if (data.getSatuanKerja() != null) {
            satuanKerja.setValue(data.getSatuanKerja().getNama());
            satuanKerja.setAttribute("satuanKerja", data.getSatuanKerja());
        }
        row.appendChild(satuanKerja);
    }

    public boolean onSave(Event event) throws Exception {
        if (nama == null || nama.getValue().trim().isEmpty()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data ruang/file",
            		"Kolom Nama ruang/file belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama ruang/file.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        Session session = HibernateUtil.currentSession();
        if (dokumenAkreditasi.getId() != null) {
            dokumenAkreditasi = (DokumenAkreditasi) session.load(DokumenAkreditasi.class, dokumenAkreditasi.getId());
        }
        dokumenAkreditasi.setKode(kode.getValue().trim());
        dokumenAkreditasi.setNama(nama.getValue().trim());
        dokumenAkreditasi.setTanggalDokumen(tanggalDokumen.getValue());
        dokumenAkreditasi.setKeterangan(keterangan.getValue());
        dokumenAkreditasi.setAkreditasi(akreditasi);
        dokumenAkreditasi.setNomorUrut(nomorUrut.getValue());
        dokumenAkreditasi.setInduk(induk);
        dokumenAkreditasi.setSatuanKerja((SatuanKerja) (satuanKerja == null ? null : satuanKerja.getAttribute("satuanKerja")));
        dokumenAkreditasi.setAktif(dokumenAkreditasi.getAktif());
        Common.refreshSaveOrUpdate(session, dokumenAkreditasi);
        updateLampiranRef(dokumenAkreditasi);
        refreshAll();
        return true;
    }

    private void updateLampiranRef(DokumenAkreditasi saved) {
        if (lainMahasiswa == null || lainMahasiswa.getId() == null || saved == null || saved.getId() == null) {
            return;
        }
        Session session = null;
        try {
            session = StreamingHibernateUtil.getInstance().currentSession();
            session.refresh(lainMahasiswa);
            lainMahasiswa.setRef(saved.getId());
            session.getTransaction().begin();
            session.update(lainMahasiswa);
            session.getTransaction().commit();
        } catch (Exception e) {
            StreamingHibernateUtil.getInstance().rollbackTransaction();
            Common.tampilErrorJikaAdmin(e);
        } finally {
            try {
                StreamingHibernateUtil.getInstance().closeSession();
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }
    }

    public Criteria initCriteria(boolean order) {
        SatuanKerja parent = (SatuanKerja) (searchparent == null ? null : searchparent.getAttribute("satuanKerja"));
        Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
        if (satuanKerjas == null) {
            satuanKerjas = new HashSet<SatuanKerja>();
        }
        if (parent != null) {
            satuanKerjas.clear();
            satuanKerjas.add(parent);
            satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
        }
        String keyword = searchnama == null ? "" : searchnama.getValue().trim();
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(DokumenAkreditasi.class)
                .add(Restrictions.eq("akreditasi", akreditasi))
                .add(Restrictions.or(!Common.getApakahAdmin() ? Restrictions.sqlRestriction("false") : Restrictions.isNull("satuanKerja"),
                        satuanKerjas == null || satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.or(parent == null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas))))
                .add(keyword.isEmpty() ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.or(Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE),
                                Restrictions.or(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE), Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE))));
        if (order) {
            criteria.addOrder(Order.asc("induk")).addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
        }
        return criteria;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void onSearchDefault(Event event) {
        if (grid == null) {
            return;
        }
        try {
            Common.initPaging(initCriteria(false), paging);
            List<DokumenAkreditasi> list = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                    .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
            ListModel model = new SimpleListModel(list);
            grid.setRowRenderer(new DokumenAkreditasiRenderer());
            grid.setModelCheckMobile(model);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            grid.setModelCheckMobile(new SimpleListModel(new java.util.ArrayList()));
        }
    }

    private void refreshAll() {
        refreshTreeModel();
        onSearchDefault(null);
    }

    private void refreshTreeModel() {
        if (tree != null) {
            dokumenAkreditasiTreeModel = new DokumenAkreditasiTreeModel(akreditasi, searchparent);
            tree.setModel(dokumenAkreditasiTreeModel);
        }
    }

    private void confirmDelete(final DokumenAkreditasi dokumen, final Treeitem treeitem) {
        try {
            MyMessageboxConfig.show("Apakah yakin ingin menghapus ruang/file ini?", "Pertanyaan",
                    MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            int answer = Integer.parseInt(event.getData().toString());
                            if (answer == MyMessageboxConfig.OK) {
                                try {
                                    Common.refreshDelete(dokumen);
                                    if (treeitem != null) {
                                        treeitem.detach();
                                    }
                                    refreshAll();
                                } catch (Exception e) {
                                    Common.tampilErrorJikaAdmin(e);
                                    showMessageSafe("Data tidak dapat dihapus karena masih berelasi dengan data lain. "
                                            + e.getMessage());
                                }
                            }
                        }
                    });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void downloadDokumen(DokumenAkreditasi dokumen) throws Exception {
        LampiranLain lampiran = LampiranLain.ambil(dokumen.getId(), DokumenAkreditasi.class.getName());
        if (lampiran != null) {
            Filedownload.save(lampiran.ambilFile(), lampiran.getKeterangan());
        } else {
            showMessageSafe("Lampiran file belum tersedia.");
        }
    }

    private void showMessageSafe(String message) {
        try {
            MyMessageboxConfig.show(message == null ? "" : message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void prosesDspaceExport(final boolean update) {
        final Tbmuser tbmuser = Common.getCurrentUser();
        final Label label = Common.displayLoadBar(new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                refreshAll();
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String cookie = DspaceCommon.login();
                    List<DokumenAkreditasi> list = initCriteria(true).list();
                    int row = 1;
                    int total = list == null || list.isEmpty() ? 1 : list.size();
                    for (DokumenAkreditasi dokumen : list) {
                        label.setValue("Ekspor DSpace " + dokumen.getNama() + " (" + Common.numberFormat.get().format((row++) * 100.0 / total) + " %)");
                        AkreditasiAction.getDspace(tbmuser, cookie, dokumen, update);
                    }
                } catch (Exception e) {
                    Common.tampilErrorJikaAdmin(e);
                }
                label.setValue("");
            }
        }).start();
    }

    private void prosesDspaceDelete() {
        try {
            MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor DSpace dokumen ini?", "Pertanyaan",
                    MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            int answer = Integer.parseInt(event.getData().toString());
                            if (answer == MyMessageboxConfig.OK) {
                                prosesDspaceDeleteThread();
                            }
                        }
                    });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void prosesDspaceDeleteThread() {
        final Label label = Common.displayLoadBar(new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                refreshAll();
                LogLoginAction.tampilDpsaceLog();
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
            	try {
                try {
                    String cookie = DspaceCommon.login();
                    Session session = HibernateUtil.currentSession();
                    List<DokumenAkreditasi> list = initCriteria(true).list();
                    int row = 1;
                    int total = list == null || list.isEmpty() ? 1 : list.size();
                    for (DokumenAkreditasi dokumen : list) {
                        label.setValue("Membatalkan ekspor " + dokumen.getNama() + " (" + Common.numberFormat.get().format((row++) * 100.0 / total) + " %)");
                        DspaceInformation info = DspaceInformation.getDspaceInformation(DokumenAkreditasi.class.getName(), dokumen.getId());
                        if (info != null) {
                            int status = DspaceInformation.delete(cookie, "items/" + info.getUuid(), info.getPostInfo());
                            if (status == 200) {
                                session = HibernateUtil.currentNativeSession();
                                session.getTransaction().begin();
                                session.delete(info);
                                session.getTransaction().commit();
                                HibernateUtil.closeSession();
                            }
                        }
                    }
                } catch (Exception e) {
                    Common.tampilErrorJikaAdmin(e);
                }
                label.setValue("");
                        	} finally {
            		ais.database.hibernate.HibernateUtil.closeSession();
            	}
            }
        }).start();
    }

    private String formatDate(java.util.Date date) {
        return date == null ? "" : Common.dateFormat.get().format(date);
    }

    private String buildFormHeroHtml(DokumenAkreditasi parent) {
        String parentName = parent == null ? "Root" : parent.getNama();
        return "<div style='padding:14px 16px; border-radius:16px; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:#fff; margin-bottom:8px; box-shadow:0 10px 24px rgba(15,23,42,.16);'>"
                + "<div style='font-size:18px; font-weight:800;'><i class='fa fa-folder-open' style='margin-right:8px;'></i>Manajemen Ruang Arsip dan File</div>"
                + "<div style='font-size:12px; opacity:.92; margin-top:4px;'>Induk: " + html(parentName) + ". Kosongkan lampiran jika item ini dipakai sebagai ruang arsip. Isi lampiran jika item ini adalah file dokumen yang dapat diunduh.</div></div>";
    }

    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String html(String value) {
        String s = value == null ? "" : value.trim();
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
