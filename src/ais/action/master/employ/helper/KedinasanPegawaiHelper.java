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

/**
 * Helper UI pengelola riwayat "Kedinasan di YTB" (Yayasan Tempat Bekerja) milik satu
 * {@link Pegawai}. Berbeda dari helper relasi lain di paket ini yang menyimpan detail sebagai
 * baris tabel terpisah, kelas ini menyimpan seluruh riwayat sebagai satu array JSON tunggal pada
 * kolom {@code kedinasan} milik entitas {@link Pegawai} — setiap entri punya id UUID sendiri
 * (dibuat di klien saat "Tambah Data" ditekan) dan field tanggal SK, nomor SK, posisi/jabatan,
 * dan keterangan. Setiap entri juga dapat memiliki satu lampiran berkas ({@link LampiranLain},
 * dikaitkan lewat kunci referensi {@code "Kedinasan_"+id}) yang dapat diunduh langsung dari grid.
 * Operasi tambah/edit/hapus seluruhnya bekerja dengan pola baca-ubah-tulis: ambil array JSON
 * saat ini, ubah, lalu tulis ulang seluruh array ke kolom {@code kedinasan} dalam satu transaksi
 * ({@link #saveToDatabase}) — bukan operasi baris database individual.
 */
public class KedinasanPegawaiHelper {

    private MyGrid grid = new MyGrid();
    private Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

    private Pegawai pegawai;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    protected LampiranLain lampiranKedinasan;

    /** Membuat helper untuk mengelola riwayat kedinasan milik {@code pegawai}. */
    public KedinasanPegawaiHelper(Pegawai pegawai) {
        this.pegawai = pegawai;
    }

    /** Mem-parsing kolom {@code kedinasan} milik pegawai menjadi {@link JSONArray}; mengembalikan array kosong bila kolom kosong/null atau gagal di-parse. */
    private JSONArray getKedinasanArray() {
        if (pegawai != null && pegawai.getKedinasan() != null && !pegawai.getKedinasan().trim().isEmpty()) {
            try {
                return new JSONArray(pegawai.getKedinasan());
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }
        return new JSONArray();
    }

    /** Renderer baris grid: tanggal SK, nomor SK, posisi, keterangan, serta tombol unduh lampiran (bila ada), edit (membuka {@link #init}), dan hapus (dengan dialog konfirmasi, memanggil {@link #deleteData}). */
    class KedinasanRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object data) throws Exception {
            final JSONObject jsonObject = (JSONObject) data;

            new Label(jsonObject.optString("tanggalSk", "")).setParent(row);
            new Label(jsonObject.optString("noSk", "")).setParent(row);
            new Label(jsonObject.optString("posisi", "")).setParent(row);
            new Label(jsonObject.optString("keterangan", "")).setParent(row);

            Hbox toolbar = new Hbox();
            
            String currentId = jsonObject.optString("id");
            LampiranLain lain = LampiranLain.ambil(pegawai.getId(), "Kedinasan_" + currentId);
            String urlDownload = lain == null ? null : lain.createLinkUri();

            // Tombol Download (Hanya muncul jika urlDownload tidak null dan tidak kosong)
            if (urlDownload != null && !urlDownload.trim().isEmpty()) {
                MyToolbarbuttonConfig btnDownload = new MyToolbarbuttonConfig("", "/img/svg/download.svg"); // Sesuaikan icon download Anda jika berbeda
                btnDownload.setTooltiptext("Download Lampiran");
                btnDownload.setHref(urlDownload);
                btnDownload.setTarget("_blank"); // Buka di tab baru / langsung download
                btnDownload.setParent(toolbar);
                
                // Menambahkan spasi sedikit antara icon agar rapi
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

    /**
     * Membangun tata letak lengkap panel riwayat kedinasan: toolbar dengan tombol "Tambah Data"
     * (membuka {@link #init(JSONObject)} dengan {@code null}) dan grid berpaginasi (10 baris/
     * halaman) yang langsung dimuat dengan data tersimpan ({@link #onSearchDefault}).
     *
     * @return {@link Borderlayout} siap ditempelkan ke jendela detail pegawai
     * @throws Exception diteruskan dari kegagalan pembangunan komponen
     */
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

        createColumn(columns, "TANGGAL SK", "15%");
        createColumn(columns, "NO. SK", "30%");
        createColumn(columns, "POSISI / JABATAN", null);
        createColumn(columns, "KETERANGAN", null);
        createColumn(columns, "", "10%");

        onSearchDefault();

        return borderlayout;
    }

    /** Menambahkan satu kolom grid dengan {@code label} dan {@code width} opsional (dilewati bila null/kosong). */
    private void createColumn(Columns columns, String label, String width) {
        MyColumnConfig column = new MyColumnConfig();
        column.setParent(columns);
        if (label != null && !label.isEmpty()) column.setLabel(label);
        if (width != null && !width.isEmpty()) column.setWidth(width);
    }

    /** Mem-parsing seluruh entri riwayat kedinasan dari kolom JSON pegawai dan menampilkannya di grid lewat {@link KedinasanRenderer}. */
    @SuppressWarnings({})
    public void onSearchDefault() {
        JSONArray arr = getKedinasanArray();
        List<JSONObject> listKedinasan = new ArrayList<JSONObject>();
        
        for (int i = 0; i < arr.length(); i++) {
            try {
                listKedinasan.add(arr.getJSONObject(i));
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }

        ListModel strset = new SimpleListModel(listKedinasan);
        grid.setRowRenderer(new KedinasanRenderer());
        grid.setModel(strset);
    }

    /**
     * Membuka jendela modal tambah/edit satu entri riwayat kedinasan. Bila {@code dataEdit}
     * {@code null}, entri baru dibuat dengan id UUID baru; bila diberikan, form diprapopulasi dari
     * nilai entri tersebut (id dipertahankan). Form memuat tanggal SK, nomor SK, posisi/jabatan,
     * keterangan, dan area unggah/unduh satu lampiran berkas ({@link LampiranLain}, dikaitkan ke
     * kunci {@code "Kedinasan_"+id}). Tombol Simpan memvalidasi tanggal SK dan nomor SK wajib
     * diisi, menulis entri ke array JSON pegawai ({@link #saveData}), mengaitkan lampiran yang
     * baru diunggah (bila ada) ke pegawai lewat sesi Hibernate streaming terpisah, lalu menyegarkan
     * grid dan menutup jendela.
     *
     * @param dataEdit entri JSON yang akan diedit, atau {@code null} untuk membuat entri baru
     * @throws Exception diteruskan dari kegagalan pembangunan komponen
     */
    public void init(final JSONObject dataEdit) throws Exception {
        final boolean isEdit = (dataEdit != null);
        final String currentId = isEdit ? dataEdit.optString("id") : UUID.randomUUID().toString();

        Borderlayout windowLayout = new ais.ui.util.MyBorderlayout();
        final MyWindow window = new MyWindow("Edit Kedinasan di YTB", "none", true);
        window.setWidth("600px");
        window.setHeight("450px");
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
        final Datebox dbTanggalSk = new Datebox();
        dbTanggalSk.setFormat("yyyy-MM-dd");
        dbTanggalSk.setWidth("90%");
        if (isEdit && !dataEdit.optString("tanggalSk").isEmpty()) {
            try {
                dbTanggalSk.setValue(sdf.parse(dataEdit.optString("tanggalSk")));
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }
        
        final Textbox txtNoSk = new Textbox(isEdit ? dataEdit.optString("noSk") : "");
        txtNoSk.setWidth("90%");
        
        final Textbox txtPosisi = new Textbox(isEdit ? dataEdit.optString("posisi") : "");
        txtPosisi.setWidth("90%");

        final Textbox txtKeterangan = new Textbox(isEdit ? dataEdit.optString("keterangan") : "");
        txtKeterangan.setWidth("90%");
        txtKeterangan.setRows(2);

        // TANGGAL SK & NO SK
        MyFormRow row1 = new MyFormRow();
        row1.setValign("top");
        row1.setParent(rows);
        
        Div divTgl = new Div();
        divTgl.appendChild(new MyLabelConfig("Tanggal SK *"));
        divTgl.appendChild(new Html("<br/>"));
        divTgl.appendChild(dbTanggalSk);
        row1.appendChild(divTgl);
        
        Div divNo = new Div();
        divNo.appendChild(new MyLabelConfig("No. SK *"));
        divNo.appendChild(new Html("<br/>"));
        divNo.appendChild(txtNoSk);
        row1.appendChild(divNo);

        // POSISI
        MyFormRow row2 = new MyFormRow();
        row2.setParent(rows);
        row2.appendChild(new MyLabelConfig("Posisi / Jabatan"));
        Div divPos = new Div();
        divPos.appendChild(txtPosisi);
        row2.appendChild(divPos);

        // KETERANGAN
        MyFormRow row3 = new MyFormRow();
        row3.setParent(rows);
        row3.appendChild(new MyLabelConfig("Keterangan"));
        Div divKet = new Div();
        divKet.appendChild(txtKeterangan);
        row3.appendChild(divKet);

        // UPLOAD FILE BERDASARKAN TEMPLATE
        lampiranKedinasan = null;
        MyFormRow row4 = new MyFormRow();
        row4.setParent(rows);
        row4.appendChild(new MyLabelConfig("Lampiran Kegiatan"));
        
        Hbox hbox = new Hbox();
        LampiranLain.createDownloadUploadFileLain(hbox, pegawai.getId(), "Kedinasan_" + currentId, "Lampiran Kegiatan", false, new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                lampiranKedinasan = (LampiranLain) arg0.getData();
            }
        });
        hbox.setParent(row4);

        Common.initKeterangan(rows, "Jika file lampiran kegiatan lebih dari satu file, zip dulu semua file tersebut");

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
                if (dbTanggalSk.getValue() == null) {
                    MyMessageboxConfig.show("Mohon maaf, Tanggal SK belum diisi. Langkah yang dapat dilakukan: (1) pilih Tanggal SK menggunakan datepicker pada form; (2) pastikan tanggal yang dipilih sudah benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
                    return;
                }
                if (txtNoSk.getValue().trim().isEmpty()) {
                    MyMessageboxConfig.show("Mohon maaf, No. SK belum diisi. Langkah yang dapat dilakukan: (1) isi kolom No. SK pada form; (2) pastikan nomor SK tidak kosong dan sesuai format yang berlaku; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
                    return;
                }

                JSONObject objToSave = new JSONObject();
                objToSave.put("id", currentId);
                objToSave.put("tanggalSk", sdf.format(dbTanggalSk.getValue()));
                objToSave.put("noSk", txtNoSk.getValue());
                objToSave.put("posisi", txtPosisi.getValue());
                objToSave.put("keterangan", txtKeterangan.getValue());

                if (saveData(objToSave, isEdit)) {
                    
                    // PENYIMPANAN LAMPIRAN BERDASARKAN TEMPLATE
                    if (lampiranKedinasan != null && lampiranKedinasan.getId() != null) {
                        Session streamSession = null;
                        try {
                            streamSession = StreamingHibernateUtil.getInstance().currentSession();

                            streamSession.refresh(lampiranKedinasan);
                            lampiranKedinasan.setRef(pegawai.getId());

                            streamSession.getTransaction().begin();
                            streamSession.update(lampiranKedinasan);
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

    /** Menyisipkan atau memperbarui (berdasarkan kecocokan id) satu entri {@code objToSave} ke dalam array riwayat kedinasan, lalu menulis ulang seluruh array ke database. @return {@code true} bila berhasil, {@code false} bila terjadi kegagalan. */
    private boolean saveData(JSONObject objToSave, boolean isEdit) {
        try {
            JSONArray arr = getKedinasanArray();
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

    /** Menghapus entri berid {@code idToDelete} dari array riwayat kedinasan, menulis ulang array ke database, dan menyegarkan grid. */
    private void deleteData(String idToDelete) {
        try {
            JSONArray arr = getKedinasanArray();
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

    /** Menulis seluruh {@code arrayToSave} sebagai string JSON ke kolom {@code kedinasan} milik pegawai, dalam transaksi Hibernate sendiri (rollback eksplisit bila gagal, sesi selalu ditutup). */
    private void saveToDatabase(JSONArray arrayToSave) throws Exception {
        pegawai.setKedinasan(arrayToSave.toString());
        
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