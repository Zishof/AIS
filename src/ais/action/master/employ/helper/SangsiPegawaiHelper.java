package ais.action.master.employ.helper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class SangsiPegawaiHelper {

    private MyGrid grid = new MyGrid();
    private Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
    
    private Pegawai pegawai;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    
    protected LampiranLain lampiranSangsi;

    public SangsiPegawaiHelper(Pegawai pegawai) {
        this.pegawai = pegawai;
    }

    private JSONArray getSangsiArray() {
        if (pegawai != null && pegawai.getSangsi() != null && !pegawai.getSangsi().trim().isEmpty()) {
            try {
                return new JSONArray(pegawai.getSangsi());
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }
        return new JSONArray();
    }

    class SangsiRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object data) throws Exception {
            final JSONObject jsonObject = (JSONObject) data;

            new Label(jsonObject.optString("tanggalMulai", "")).setParent(row);
            new Label(jsonObject.optString("tanggalSampai", "")).setParent(row);
            new Label(jsonObject.optString("nomor", "")).setParent(row);
            new Label(jsonObject.optString("nama", "")).setParent(row);
            new Label(jsonObject.optString("keterangan", "")).setParent(row);

            Hbox toolbar = new Hbox();
            
            String currentId = jsonObject.optString("id");
            LampiranLain lain = LampiranLain.ambil(pegawai.getId(), "Sangsi_" + currentId);
            String urlDownload = lain == null ? null : lain.createLinkUri();

            // Tombol Download
            if (urlDownload != null && !urlDownload.trim().isEmpty()) {
                MyToolbarbuttonConfig btnDownload = new MyToolbarbuttonConfig("", "/img/svg/download.svg"); 
                btnDownload.setTooltiptext("Download Lampiran");
                btnDownload.setHref(urlDownload);
                btnDownload.setTarget("_blank"); 
                btnDownload.setParent(toolbar);
                
                new org.zkoss.zul.Space().setParent(toolbar);
            }
            
            // Tombol Edit
            MyToolbarbuttonConfig btnEdit = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
            btnEdit.setTooltiptext("Ubah Data");
            btnEdit.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    init(jsonObject);
                }
            });
            btnEdit.setParent(toolbar);

            // Tombol Hapus
            MyToolbarbuttonConfig btnDelete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
            btnDelete.setTooltiptext("Hapus Data");
            btnDelete.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini?", "Pertanyaan",
                            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
                            new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int i = new Integer(event.getData().toString());
                                    if (i == MyMessageboxConfig.OK) {
                                        try {
                                            deleteData(jsonObject.optString("id"));
                                        } catch (Exception e) {
                                            Common.tampilErrorJikaAdmin(e);
                                        }
                                    }
                                }
                            });
                }
            });
            btnDelete.setParent(toolbar);

            toolbar.setParent(row);
        }
    }

    public Borderlayout display() throws Exception {
        North north = new North();
        Center center = new Center();

        Common.clear(borderlayout);

        borderlayout.setWidth("100%");
        center.setParent(borderlayout);
        ais.ui.util.ZkCompat.setFlex(center, true);

        north.setParent(borderlayout);

        Div div = new Div();
        div.setParent(north);

        Toolbar toolbar = new Toolbar();
        toolbar.setParent(div);

        MyToolbarbuttonConfig btnTambah = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
        toolbar.appendChild(btnTambah);
        btnTambah.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                init(null);
            }
        });

        grid = new MyGrid();
        grid.setMold("paging");
        grid.setPageSize(10);
        grid.getPagingChild().setMold("os");
        grid.setParent(center);

        Columns columns = new Columns();
        columns.setParent(grid);

        createColumn(columns, "TGL MULAI", "12%");
        createColumn(columns, "TGL SAMPAI", "12%");
        createColumn(columns, "NOMOR SURAT", "20%");
        createColumn(columns, "NAMA SANGSI", null);
        createColumn(columns, "KETERANGAN", null);
        createColumn(columns, "", "10%");

        onSearchDefault();

        return borderlayout;
    }

    private void createColumn(Columns columns, String label, String width) {
        MyColumnConfig column = new MyColumnConfig();
        column.setParent(columns);
        if (label != null && !label.isEmpty()) column.setLabel(label);
        if (width != null && !width.isEmpty()) column.setWidth(width);
    }

    @SuppressWarnings({})
    public void onSearchDefault() {
        JSONArray arr = getSangsiArray();
        List<JSONObject> listSangsi = new ArrayList<JSONObject>();
        
        for (int i = 0; i < arr.length(); i++) {
            try {
                listSangsi.add(arr.getJSONObject(i));
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }

        ListModel strset = new SimpleListModel(listSangsi);
        grid.setRowRenderer(new SangsiRenderer());
        grid.setModel(strset);
    }

    public void init(final JSONObject dataEdit) throws Exception {
        final boolean isEdit = (dataEdit != null);
        final String currentId = isEdit ? dataEdit.optString("id") : UUID.randomUUID().toString();

        Borderlayout windowLayout = new ais.ui.util.MyBorderlayout();
        final MyWindow window = new MyWindow("Edit Sangsi Pegawai", "none", true);
        window.setWidth("650px");
        window.setHeight("480px");
        ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
        window.appendChild(windowLayout);

        Center center = new Center();
        center.setParent(windowLayout);
        ais.ui.util.ZkCompat.setFlex(center, true);

        final MyGrid formGrid = new MyGrid();
        formGrid.setWidth("100%");
        formGrid.setParent(center);

        Columns columns = new Columns();
        columns.setParent(formGrid);

        createColumn(columns, null, "30%");
        createColumn(columns, null, null);

        final Rows rows = new Rows();
        rows.setParent(formGrid);

        // FORM FIELDS
        final Datebox dbTanggalMulai = new Datebox();
        dbTanggalMulai.setFormat("yyyy-MM-dd");
        dbTanggalMulai.setWidth("90%");
        if (isEdit && !dataEdit.optString("tanggalMulai").isEmpty()) {
            try {
                dbTanggalMulai.setValue(sdf.parse(dataEdit.optString("tanggalMulai")));
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }
        
        final Datebox dbTanggalSampai = new Datebox();
        dbTanggalSampai.setFormat("yyyy-MM-dd");
        dbTanggalSampai.setWidth("90%");
        if (isEdit && !dataEdit.optString("tanggalSampai").isEmpty()) {
            try {
                dbTanggalSampai.setValue(sdf.parse(dataEdit.optString("tanggalSampai")));
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }
        
        final Textbox txtNomor = new Textbox(isEdit ? dataEdit.optString("nomor") : "");
        txtNomor.setWidth("90%");
        
        final Textbox txtNama = new Textbox(isEdit ? dataEdit.optString("nama") : "");
        txtNama.setWidth("90%");

        final Textbox txtKeterangan = new Textbox(isEdit ? dataEdit.optString("keterangan") : "");
        txtKeterangan.setWidth("90%");
        txtKeterangan.setRows(3);

        // TANGGAL MULAI & TANGGAL SAMPAI
        MyFormRow row1 = new MyFormRow();
        row1.setValign("top");
        row1.setParent(rows);
        
        Div divTglMulai = new Div();
        divTglMulai.appendChild(new MyLabelConfig("Tanggal Mulai *"));
        divTglMulai.appendChild(new Html("<br/>"));
        divTglMulai.appendChild(dbTanggalMulai);
        row1.appendChild(divTglMulai);
        
        Div divTglSampai = new Div();
        divTglSampai.appendChild(new MyLabelConfig("Tanggal Sampai"));
        divTglSampai.appendChild(new Html("<br/>"));
        divTglSampai.appendChild(dbTanggalSampai);
        row1.appendChild(divTglSampai);

        // NOMOR SURAT / SK
        MyFormRow row2 = new MyFormRow();
        row2.setParent(rows);
        row2.appendChild(new MyLabelConfig("Nomor Surat/SK *"));
        Div divNo = new Div();
        divNo.appendChild(txtNomor);
        row2.appendChild(divNo);

        // NAMA SANGSI
        MyFormRow row3 = new MyFormRow();
        row3.setParent(rows);
        row3.appendChild(new MyLabelConfig("Nama/Jenis Sangsi"));
        Div divNama = new Div();
        divNama.appendChild(txtNama);
        row3.appendChild(divNama);

        // KETERANGAN
        MyFormRow row4 = new MyFormRow();
        row4.setParent(rows);
        row4.appendChild(new MyLabelConfig("Keterangan"));
        Div divKet = new Div();
        divKet.appendChild(txtKeterangan);
        row4.appendChild(divKet);

        // UPLOAD FILE BERDASARKAN TEMPLATE
        lampiranSangsi = null;
        MyFormRow row5 = new MyFormRow();
        row5.setParent(rows);
        row5.appendChild(new MyLabelConfig("Lampiran Sangsi"));
        
        Hbox hbox = new Hbox();
        LampiranLain.createDownloadUploadFileLain(hbox, pegawai.getId(), "Sangsi_" + currentId, "Lampiran Sangsi", false, new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                lampiranSangsi = (LampiranLain) arg0.getData();
            }
        });
        hbox.setParent(row5);

        Common.initKeterangan(rows, "Jika file lampiran lebih dari satu file, zip dulu semua file tersebut");

        // FOOTER BUTTONS
        South south = new South();
        south.setParent(windowLayout);
        
        Toolbar toolbar = new Toolbar();
        toolbar.setAlign("end");
        toolbar.setParent(south);

        Button btnSimpan = new Button("Simpan");
        btnSimpan.setSclass("btn btn-primary");
        btnSimpan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                if (dbTanggalMulai.getValue() == null) {
                    MyMessageboxConfig.show("Mohon maaf, Tanggal Mulai belum diisi. Langkah yang dapat dilakukan: (1) pilih Tanggal Mulai menggunakan datepicker; (2) pastikan tanggal yang dipilih sudah benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
                    return;
                }
                if (txtNomor.getValue().trim().isEmpty()) {
                    MyMessageboxConfig.show("Mohon maaf, Nomor Surat/SK belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nomor Surat/SK; (2) pastikan nomor tidak kosong dan sesuai format; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
                    return;
                }

                JSONObject objToSave = new JSONObject();
                objToSave.put("id", currentId);
                objToSave.put("tanggalMulai", sdf.format(dbTanggalMulai.getValue()));
                objToSave.put("tanggalSampai", dbTanggalSampai.getValue() != null ? sdf.format(dbTanggalSampai.getValue()) : "");
                objToSave.put("nomor", txtNomor.getValue());
                objToSave.put("nama", txtNama.getValue());
                objToSave.put("keterangan", txtKeterangan.getValue());

                if (saveData(objToSave, isEdit)) {
                    
                    // PENYIMPANAN LAMPIRAN BERDASARKAN TEMPLATE
                    if (lampiranSangsi != null && lampiranSangsi.getId() != null) {
                        Session streamSession = null;
                        try {
                            streamSession = StreamingHibernateUtil.getInstance().currentSession();

                            streamSession.refresh(lampiranSangsi);
                            lampiranSangsi.setRef(pegawai.getId());

                            streamSession.getTransaction().begin();
                            streamSession.update(lampiranSangsi);
                            streamSession.getTransaction().commit();

                        } catch (Exception e) {
                            StreamingHibernateUtil.getInstance().rollbackTransaction();
                            Common.tampilErrorJikaAdmin(e);
                        } finally {
                            StreamingHibernateUtil.getInstance().closeSession();
                        }
                    }

                    onSearchDefault();
                    window.detach();
                }
            }
        });
        btnSimpan.setParent(toolbar);

        Button btnKembali = new Button("Close");
        btnKembali.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                window.detach();
            }
        });
        btnKembali.setParent(toolbar);

        window.doModal();
    }

    private boolean saveData(JSONObject objToSave, boolean isEdit) {
        try {
            JSONArray arr = getSangsiArray();
            JSONArray newArr = new JSONArray();
            
            boolean updated = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject existingObj = arr.getJSONObject(i);
                if (existingObj.optString("id").equals(objToSave.optString("id"))) {
                    newArr.put(objToSave);
                    updated = true;
                } else {
                    newArr.put(existingObj);
                }
            }

            if (!updated) {
                newArr.put(objToSave);
            }

            saveToDatabase(newArr);
            return true;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return false;
        }
    }

    private void deleteData(String idToDelete) {
        try {
            JSONArray arr = getSangsiArray();
            JSONArray newArr = new JSONArray();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject existingObj = arr.getJSONObject(i);
                if (!existingObj.optString("id").equals(idToDelete)) {
                    newArr.put(existingObj);
                }
            }

            saveToDatabase(newArr);
            onSearchDefault();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void saveToDatabase(JSONArray arrayToSave) throws Exception {
        pegawai.setSangsi(arrayToSave.toString());
        
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            session.getTransaction().begin();
            session.update(pegawai);
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            throw e; 
        } finally {
            if (session != null) {
                session.clear();
                session.disconnect();
                session.close();
            }
        }
    }
}