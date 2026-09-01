package ais.action.master.obe;

import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Matakuliah;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyRowRenderer;
import ais.action.master.helper.GenerateAiHelper;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

/**
 * Controller/action ZK untuk capaian pembelajaran lulusan. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * ObeBaseAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox kode}, {@code Textbox nama},
 * {@code Textbox keterangan}, {@code Combobox fakultas}, {@code Combobox jurusan}, {@code
 * AmbilDataMatakuliahBanbox khususBuatMk}, {@code MyDoublebox txtBobot}, {@code MyDoublebox txtMinimal};
 * inisialisasi/lifecycle ({@code doAfterCompose()}, {@code init()}, {@code initForm()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code reloadFormula()}, {@code reloadDataFormula()}, {@code onSearchDefault()}); mutasi
 * data ({@code onSave()}); operasi domain lain ({@code onAdd()}, {@code onAddExternal()}, {@code
 * tambahSubCpmkViaAi()}, {@code showRiwayatCqi()}, {@code escCpmk()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see ObeBaseAction
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class CapaianPembelajaranLulusanAction extends ObeBaseAction {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox      kode;
    private Textbox      nama;
    private Textbox      keterangan;
    private Combobox     fakultas;
    private Combobox     jurusan;
    private AmbilDataMatakuliahBanbox khususBuatMk;
    private MyDoublebox  txtBobot;
    private MyDoublebox  txtMinimal;

    // Sub-CPMK formula
    private JSONArray array;
    private Row       rowFormula;

    private CapaianPembelajaranLulusan capaianPembelajaranLulusan;
    private EventListener eventListener;
    private Matakuliah contextMkForAdd;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        String[] contents = {"id", "jurusan", "nama", "keterangan", "formula", "khususBuatMk", "bobot", "minimal", "aktif"};
        initCommon(comp, CapaianPembelajaranLulusan.class, contents);
    }

    // ── Tambah / edit ─────────────────────────────────────────────────────────

    public void onAdd(Event event) throws Exception {
        initForm(new CapaianPembelajaranLulusan());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        initForm((CapaianPembelajaranLulusan) obj);
    }

    public static void onAddExternal(Event event, EventListener listener,
            CapaianPembelajaranLulusan cpl, Matakuliah khususMk) throws Exception {
        CapaianPembelajaranLulusanAction action = new CapaianPembelajaranLulusanAction();
        action.eventListener = listener;
        action.addWindow = new MyWindow();
        ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
                .appendChild(action.addWindow);
        action.addWindow.setHeight("98%");
        action.addWindow.setWidth("900px");
        // Untuk form TAMBAH BARU: dropdown kosong (default); checkbox "Khusus MK Ini" bila
        // dicentang → otomatis mengisi dengan khususMk (MK konteks saat ini).
        action.contextMkForAdd = khususMk;
        action.initForm(cpl);
        action.lockFakultasJurusanIfSelected(action.fakultas, action.jurusan);
        action.addWindow.setVisible(true);
        action.addWindow.onModal();
    }

    private void initForm(final CapaianPembelajaranLulusan cpl) throws Exception {
        this.capaianPembelajaranLulusan = cpl;

        fakultas = new Combobox();
        jurusan  = new Combobox();
        Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

        FormContext ctx = buildFormBorderlayout(
                "Pendataan Capaian Pembelajaran Lulusan yang dibebankan pada matakuliah (CPMK)");
        Rows rows = ctx.rows;

        kode = new Textbox(cpl.getKode());
        nama = new Textbox(cpl.getNama());
        nama.setRows(3);
        addKodeNamaFakultasJurusanRows(rows, kode, nama, fakultas, jurusan,
                cpl.getJurusan(), "Kode CPMK", "Isi CPMK");

        khususBuatMk = addKhususMkRow(rows, cpl.getKhususBuatMk(), contextMkForAdd);
        keterangan   = addKeteranganRow(rows, cpl.getKeterangan());
        keterangan.setRows(2);

        // Bobot & Minimal Ketercapaian — satu baris berdampingan agar form lebih ringkas
        txtBobot = new MyDoublebox(cpl.getBobot());
        txtBobot.setWidth("75px");
        txtBobot.setTooltiptext("Bobot CPMK ini dalam penilaian MK (%)");

        txtMinimal = new MyDoublebox(cpl.getMinimal());
        txtMinimal.setWidth("75px");
        txtMinimal.setTooltiptext("Persentase minimal ketercapaian CPMK (%)");

        org.zkoss.zul.Hbox hbBobot = new org.zkoss.zul.Hbox();
        hbBobot.setAlign("center");
        hbBobot.setSpacing("12px");
        hbBobot.appendChild(txtBobot);
        hbBobot.appendChild(new Label("  Minimal Ketercapaian (%):"));
        hbBobot.appendChild(txtMinimal);
        addFormRow(rows, "Bobot CPMK (%)", hbBobot);

        // Header Sub-CPMK
        MyFormRow headerRow = new MyFormRow();
        ZkCompat.setSpans(headerRow, "2");
        headerRow.setParent(rows);
        headerRow.appendChild(new ais.ui.util.MyLabelBoldConfig("SUB-CPMK"));

        // Area formula Sub-CPMK
        MyFormRow formulaContainer = new MyFormRow();
        ZkCompat.setSpans(formulaContainer, "2");
        formulaContainer.setParent(rows);

        array      = new JSONArray(cpl.getFormula());
        rowFormula = Common.tampilanScroll1(formulaContainer);
        reloadFormula(rowFormula, array);

        // CQI history button — only for saved CPMKs that already have an ID
        if (cpl.getId() != null && cpl.getId() > 0) {
            MyFormRow cqiRow = new MyFormRow();
            ZkCompat.setSpans(cqiRow, "2");
            cqiRow.setParent(rows);
            MyToolbarbuttonConfig cqiBtn = new MyToolbarbuttonConfig(
                    "Riwayat CQI Loop 1", "/img/svg/list.svg");
            cqiBtn.setTooltiptext("Lihat riwayat evaluasi & perbaikan (CQI) lintas semester untuk CPMK ini");
            cqiBtn.setStyle("background:#0369a1;color:#fff;border-radius:4px;padding:3px 14px;border:0;margin-top:4px");
            cqiBtn.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    showRiwayatCqi(cpl);
                }
            });
            cqiBtn.setParent(cqiRow);
        }

        buildSouthToolbar(ctx.borderlayout, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                    if (eventListener != null) {
                        eventListener.onEvent(new Event("", addWindow,
                                CapaianPembelajaranLulusanAction.this.capaianPembelajaranLulusan));
                    }
                }
            }
        });
        attachAndShow(ctx.borderlayout);
    }

    // ── Sub-CPMK formula ─────────────────────────────────────────────────────

    public void reloadFormula(final Row rowFormula, final JSONArray array) throws Exception {
        final MyFormRow dataRow = new MyFormRow();
        if (rowFormula.getGrid() != null) {
            rowFormula.getGrid().setWidth("100%");
            rowFormula.getGrid().setStyle("border:0px;background:transparent;width:100%;");
        }

        MyToolbarbuttonConfig addBtn = new MyToolbarbuttonConfig("Tambah Sub-CPMK", "/img/svg/addthis.svg");
        addBtn.setTooltiptext("Tambah Sub-CPMK");
        addBtn.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                JSONObject obj = new JSONObject();
                obj.put("kode", "");
                obj.put("nama", "");
                obj.put("key", String.valueOf(Math.abs(Common.randLong())));
                array.put(obj);
                reloadDataFormula(dataRow, array);
            }
        });
        addBtn.setParent(rowFormula);

        MyToolbarbuttonConfig aiBtn = new MyToolbarbuttonConfig("Tambah via AI", "/img/svg/sparkles.svg");
        aiBtn.setTooltiptext("Tambah Sub-CPMK via AI");
        aiBtn.setStyle("color:#ffffff;background-color:#7c3aed;border-radius:6px;margin-left:6px;");
        aiBtn.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                tambahSubCpmkViaAi(dataRow, array);
            }
        });
        aiBtn.setParent(rowFormula);

        dataRow.setParent(rowFormula.getParent());
        dataRow.setStyle("border:0px;background:transparent;width:100%;");
        reloadDataFormula(dataRow, array);
    }

    /** Generate Sub-CPMK via AI (streaming) berdasar Isi CPMK + Matakuliah, lalu tambahkan ke formula. */
    private void tambahSubCpmkViaAi(final Row dataRow, final JSONArray array) throws Exception {
        String cpmkTeks = (nama != null && nama.getValue() != null) ? nama.getValue().trim() : "";
        if (cpmkTeks.length() == 0 && capaianPembelajaranLulusan != null
                && capaianPembelajaranLulusan.getNama() != null) {
            cpmkTeks = capaianPembelajaranLulusan.getNama().trim();
        }
        String mkNama = "";
        try {
            Object mkObj = khususBuatMk == null ? null : khususBuatMk.getAttribute("matakuliah");
            if (mkObj instanceof Matakuliah && ((Matakuliah) mkObj).getNama() != null) {
                mkNama = ((Matakuliah) mkObj).getNama();
            } else if (capaianPembelajaranLulusan != null && capaianPembelajaranLulusan.getKhususBuatMk() != null
                    && capaianPembelajaranLulusan.getKhususBuatMk().getNama() != null) {
                mkNama = capaianPembelajaranLulusan.getKhususBuatMk().getNama();
            }
        } catch (Exception e) {
        }

        StringBuilder p = new StringBuilder();
        p.append("Berdasarkan CPMK (Capaian Pembelajaran Matakuliah) berikut, buatkan 3 sampai 5 Sub-CPMK: ");
        p.append("kemampuan akhir yang SPESIFIK, TERUKUR, dan menyusun CPMK ini.\n");
        if (mkNama.length() > 0) {
            p.append("Matakuliah: ").append(mkNama).append("\n");
        }
        p.append("CPMK: ").append(cpmkTeks).append("\n\n");
        p.append("Keluarkan HANYA JSON array valid (tanpa teks/markdown lain), format persis:\n");
        p.append("[{\"kode\":\"Sub-CPMK1\",\"nama\":\"kemampuan akhir...\",\"bobot\":25}]\n");
        p.append("Total seluruh bobot = 100.\n");
        final String prompt = p.toString();

        GenerateAiHelper.jalankanAiStreaming(Common.getBahasaConfig("Tambah Sub-CPMK via AI"), prompt,
                new GenerateAiHelper.HasilAi() {
                    @Override
                    public void selesai(String resp) throws Exception {
                        if (resp == null) {
                            return;
                        }
                        int a = resp.indexOf('[');
                        int b = resp.lastIndexOf(']');
                        if (a < 0 || b <= a) {
                            return;
                        }
                        JSONArray arr = new JSONArray(resp.substring(a, b + 1));
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.optJSONObject(i);
                            if (o == null) {
                                continue;
                            }
                            String nm = o.optString("nama", "").trim();
                            if (nm.length() == 0) {
                                continue;
                            }
                            String kd = o.optString("kode", "").trim();
                            double bobot = o.optDouble("bobot", 0.0);
                            JSONObject obj = new JSONObject();
                            obj.put("kode", kd);
                            obj.put("nama", nm);
                            obj.put("bobot", bobot);
                            obj.put("key", String.valueOf(Math.abs(Common.randLong())));
                            array.put(obj);
                        }
                        reloadDataFormula(dataRow, array);
                    }
                });
    }

    public void reloadDataFormula(final Row dataRow, final JSONArray array) throws Exception {
        Common.clear(dataRow);

        org.zkoss.zul.Div wrapper = new org.zkoss.zul.Div();
        wrapper.setWidth("100%");
        wrapper.setStyle("overflow-x:auto;overflow-y:hidden;padding-bottom:4px;");
        wrapper.setParent(dataRow);

        Grid grid = new Grid();
        grid.setSclass("dgrid obe-sub-cpmk-editor");
        grid.setWidth("100%");
        grid.setStyle("min-width:720px;");
        grid.setFixedLayout(true);
        grid.setParent(wrapper);

        Columns cols = new Columns();
        cols.setParent(grid);
        MyColumnConfig cKode = new MyColumnConfig("Kode Sub-CPMK");
        cKode.setWidth("150px");
        cKode.setParent(cols);
        MyColumnConfig cNama = new MyColumnConfig("Sub-CPMK");
        cNama.setWidth("390px");
        cNama.setParent(cols);
        MyColumnConfig cBobot = new MyColumnConfig("Bobot (%)");
        cBobot.setWidth("110px");
        cBobot.setParent(cols);
        MyColumnConfig cHps = new MyColumnConfig("Hapus");
        cHps.setWidth("70px");
        cHps.setAlign("center");
        cHps.setParent(cols);

        Rows rows = new Rows();
        rows.setParent(grid);

        for (int i = 0; i < array.length(); i++) {
            final int index = i;
            final JSONObject obj = array.getJSONObject(i);
            if (obj.isNull("key")) continue;

            String namaVal  = obj.isNull("nama")  ? "" : obj.get("nama") + "";
            String kodeVal  = obj.isNull("kode")  ? "" : obj.get("kode") + "";
            double bobotVal = obj.isNull("bobot") ? 0.0
                    : (obj.get("bobot") instanceof Number ? ((Number)obj.get("bobot")).doubleValue() : 0.0);

            MyFormRow r = new MyFormRow();
            r.setValign("top");
            r.setParent(rows);

            final MyTextbox kodeText = new MyTextbox(kodeVal);
            kodeText.setWidth("100%");
            kodeText.setRows(2);
            r.appendChild(kodeText);

            final MyTextbox namaText = new MyTextbox(namaVal);
            namaText.setWidth("100%");
            namaText.setRows(2);
            r.appendChild(namaText);

            final MyDoublebox bobotBox = new MyDoublebox(bobotVal > 0 ? bobotVal : null);
            bobotBox.setWidth("95px");
            bobotBox.setTooltiptext("Bobot Sub-CPMK ini dalam CPMK (%)");
            r.appendChild(bobotBox);

            // Sinkronisasi perubahan ke JSONObject
            EventListener syncListener = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    obj.put("kode", kodeText.getValue());
                    obj.put("nama", namaText.getValue());
                    Double bv = bobotBox.getValue();
                    obj.put("bobot", bv != null ? bv : 0.0);
                }
            };
            kodeText.addEventListener("onChange", syncListener);
            namaText.addEventListener("onChange", syncListener);
            bobotBox.addEventListener("onChange", syncListener);
			kodeText.addEventListener("onBlur", syncListener);
			namaText.addEventListener("onBlur", syncListener);
			bobotBox.addEventListener("onBlur", syncListener);

            MyToolbarbuttonConfig delBtn = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
            delBtn.setTooltiptext("Hapus Data");
            delBtn.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    ais.ui.util.MyMessageboxConfig.show(
                            "Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
                            ais.ui.util.MyMessageboxConfig.OK | ais.ui.util.MyMessageboxConfig.CANCEL,
                            ais.ui.util.MyMessageboxConfig.QUESTION,
                            new EventListener() {
                                @Override
                                public void onEvent(Event event) throws Exception {
                                    int btn = Integer.parseInt(event.getData().toString());
                                    if (btn == ais.ui.util.MyMessageboxConfig.OK) {
                                        try {
                                            array.put(index, new JSONObject());
                                            reloadDataFormula(dataRow, array);
                                        } catch (Exception e) {
                                            Common.tampilErrorJikaAdmin(e);
                                            ais.ui.util.MyMessageboxConfig.show(
                                                    "Data tidak dapat dihapus karena berelasi dengan data lain: "
                                                            + e.getMessage());
                                        }
                                    }
                                }
                            });
                }
            });
            delBtn.setParent(r);
        }
    }

    // ── Simpan ────────────────────────────────────────────────────────────────

    public boolean onSave(Event event) throws Exception {
        if (!validateNamaRequired(nama, "Capaian Pembelajaran Lulusan (CPMK)")) return false;
        if (!validateJurusanRequired(jurusan)) return false;

        Session session = HibernateUtil.currentSession();
        if (capaianPembelajaranLulusan.getId() != null) {
            capaianPembelajaranLulusan = (CapaianPembelajaranLulusan)
                    session.load(CapaianPembelajaranLulusan.class, capaianPembelajaranLulusan.getId());
        }
        capaianPembelajaranLulusan.setKode(kode.getValue().trim());
        capaianPembelajaranLulusan.setJurusan((Jurusan) jurusan.getSelectedItem().getValue());
        capaianPembelajaranLulusan.setNama(nama.getValue());
        capaianPembelajaranLulusan.setKeterangan(keterangan.getValue());
        capaianPembelajaranLulusan.setPerguruanTinggi(perguruanTinggi);
        capaianPembelajaranLulusan.setFormula(array.toString());
        capaianPembelajaranLulusan.setKhususBuatMk((Matakuliah) khususBuatMk.getAttribute("matakuliah"));
        capaianPembelajaranLulusan.setBobot(txtBobot.getValue());
        capaianPembelajaranLulusan.setMinimal(txtMinimal.getValue());

        Common.refreshSaveOrUpdate(session, capaianPembelajaranLulusan);
        return true;
    }

    // ── Pencarian & grid ─────────────────────────────────────────────────────

    @Override
    public Criteria initCriteria(boolean order) {
        return buildBaseCriteria(HibernateUtil.currentSession(), CapaianPembelajaranLulusan.class,
                order, true, "kode", "nama");
    }

    @Override
    public void onSearchDefault(Event event) {
        if (searchnama == null) return;
        executeSearch(initCriteria(false), initCriteria(true), new CpmkRenderer());
    }

    // ── Renderer ─────────────────────────────────────────────────────────────

    /**
     * Renderer lokal untuk layar/komponen {@link CapaianPembelajaranLulusanAction}. Kelas ini menerjemahkan satu
     * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link CapaianPembelajaranLulusanAction} dan dapat
     * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see CapaianPembelajaranLulusanAction
     */
    class CpmkRenderer extends MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final CapaianPembelajaranLulusan cpl = (CapaianPembelajaranLulusan) obj;

            new Label(cpl.getKode()).setParent(row);
            namaCellRingkas(CapaianPembelajaranLulusan.class, cpl, cpl.getNama()).setParent(row);

            // Kolom Sub-CPMK: daftar dari formula JSON. Tinggi tabel-dalam-tabel
            // dibatasi + digulir SECARA GLOBAL lewat CSS (lihat css_utama.css blok
            // "Tabel di dalam tabel") agar baris CPMK tidak memanjang ke bawah saat
            // Sub-CPMK-nya banyak. Tidak perlu pembungkus khusus di sini.
            Grid subGrid = new Grid();
            subGrid.setSclass("dgrid");
            subGrid.setWidth("100%");
            subGrid.setParent(row);
            Columns cols = new Columns();
            cols.setParent(subGrid);
            MyColumnConfig ck = new MyColumnConfig("Kode SUB-CPMK");
            ck.setWidth("30%");
            ck.setParent(cols);
            new MyColumnConfig("SUB-CPMK").setParent(cols);
            Rows subRows = new Rows();
            subRows.setParent(subGrid);

            JSONArray arr = new JSONArray(cpl.getFormula());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject jo = arr.getJSONObject(i);
                if (jo.isNull("key")) continue;
                String kodeVal = jo.isNull("kode") ? "" : jo.get("kode") + "";
                String namaVal = jo.isNull("nama") ? "" : jo.get("nama") + "";
                MyFormRow sr = new MyFormRow();
                sr.setParent(subRows);
                sr.appendChild(new MyLabelAgakKecil(kodeVal));
                sr.appendChild(new MyLabelAgakKecil(namaVal));
            }

            new Label(cpl.getJurusan() == null ? "" : cpl.getJurusan().getNama()).setParent(row);
            new Label(cpl.getKhususBuatMk() == null ? "" : cpl.getKhususBuatMk().getNama()).setParent(row);
            ringkasanKeterangan(cpl.getKeterangan()).setParent(row);
            // Bobot & Minimal
            new Label(cpl.getBobot() != null && cpl.getBobot() > 0
                    ? String.format("%.0f%%", cpl.getBobot()) : "-").setParent(row);
            new Label(cpl.getMinimal() != null && cpl.getMinimal() > 0
                    ? String.format("%.0f%%", cpl.getMinimal()) : "-").setParent(row);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(cpl.getAktif());
            checkbox.setParent(row);
            row.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    cpl.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(cpl);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, cpl,
                    CapaianPembelajaranLulusanAction.this).setParent(row);
        }
    }

    private static void showRiwayatCqi(CapaianPembelajaranLulusan cpl) throws Exception {
        final String kode = cpl.getKode() == null ? "" : cpl.getKode().trim();
        if (kode.isEmpty()) return;

        org.hibernate.Session sess = ais.database.hibernate.HibernateUtil.openSession();
        final StringBuilder sb = new StringBuilder();
        try {
            @SuppressWarnings({"deprecation", "unchecked"})
            java.util.List<ais.database.model.Perkuliahan> list =
                    (java.util.List<ais.database.model.Perkuliahan>) sess.createCriteria(
                            ais.database.model.Perkuliahan.class)
                    .add(org.hibernate.criterion.Restrictions.ilike(
                            "cqiData", kode, org.hibernate.criterion.MatchMode.ANYWHERE))
                    .addOrder(org.hibernate.criterion.Order.desc("tahunAjaran"))
                    .list();

            sb.append("<style>");
            sb.append(".rw{font-family:Arial,sans-serif;padding:14px;color:#1e293b}");
            sb.append(".rw h3{margin:0 0 10px;color:#0369a1;font-size:13pt}");
            sb.append(".rw-t{border-collapse:collapse;width:100%;font-size:9pt}");
            sb.append(".rw-t th{background:#dbeafe;padding:5px 7px;border:1px solid #cbd5e1;text-align:left;white-space:nowrap}");
            sb.append(".rw-t td{padding:4px 7px;border:1px solid #e2e8f0;vertical-align:top}");
            sb.append(".rw-t tr:nth-child(even){background:#f8fafc}");
            sb.append(".s-done{background:#dcfce7;color:#166534;font-weight:bold;padding:2px 6px;border-radius:4px}");
            sb.append(".s-run{background:#fef9c3;color:#854d0e;font-weight:bold;padding:2px 6px;border-radius:4px}");
            sb.append(".s-plan{background:#f1f5f9;color:#475569;padding:2px 6px;border-radius:4px}");
            sb.append(".empty{color:#94a3b8;font-style:italic;padding:10px 0}");
            sb.append("</style>");
            sb.append("<div class='rw'>");
            sb.append("<h3>Riwayat CQI Loop 1: ").append(escCpmk(kode)).append("</h3>");
            sb.append("<p style='font-size:9pt;color:#64748b;margin:0 0 10px'>")
              .append(escCpmk(cpl.getNama())).append("</p>");

            int totalEntries = 0;
            final StringBuilder rows2 = new StringBuilder();
            for (ais.database.model.Perkuliahan p : list) {
                String json = p.getCqiData();
                if (json == null || json.trim().isEmpty()) continue;
                try {
                    org.json.JSONArray arr = new org.json.JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject entry = arr.getJSONObject(i);
                        if (!kode.equalsIgnoreCase(entry.optString("cpmk"))) continue;

                        ais.database.model.KurikulumPunyaMatakuliah kpm = p.getKurikulumPunyaMatakuliah();
                        String mkKode = (kpm != null && kpm.getMatakuliah() != null && kpm.getMatakuliah().getKode() != null)
                                ? kpm.getMatakuliah().getKode() : "?";
                        String mkNama = (kpm != null && kpm.getMatakuliah() != null && kpm.getMatakuliah().getNama() != null)
                                ? kpm.getMatakuliah().getNama() : "";
                        String ta   = p.getTahunAjaran() == null ? "-" : p.getTahunAjaran();
                        String smtr = p.getGanjilGenap()  == null ? ""  : p.getGanjilGenap();
                        String status = entry.optString("status", "");
                        String sCss = "Selesai".equalsIgnoreCase(status) ? "s-done"
                                : ("Berjalan".equalsIgnoreCase(status) || "In Progress".equalsIgnoreCase(status)) ? "s-run"
                                : "s-plan";

                        rows2.append("<tr>");
                        rows2.append("<td style='white-space:nowrap'>").append(escCpmk(ta)).append(" ").append(escCpmk(smtr)).append("</td>");
                        rows2.append("<td><b>").append(escCpmk(mkKode)).append("</b><br><small>").append(escCpmk(mkNama)).append("</small></td>");
                        rows2.append("<td>").append(escCpmk(entry.optString("masalah"))).append("</td>");
                        rows2.append("<td>").append(escCpmk(entry.optString("analisis"))).append("</td>");
                        rows2.append("<td>").append(escCpmk(entry.optString("rencana"))).append("</td>");
                        rows2.append("<td>").append(escCpmk(entry.optString("pj"))).append("</td>");
                        rows2.append("<td>").append(escCpmk(entry.optString("targetWaktu"))).append("</td>");
                        rows2.append("<td><span class='").append(sCss).append("'>").append(escCpmk(status)).append("</span></td>");
                        rows2.append("</tr>");
                        totalEntries++;
                    }
                } catch (Exception ex3) { ais.common.ErrorAuditUtil.record(ex3, "auto-audit(empty-catch) src/ais/action/master/obe/CapaianPembelajaranLulusanAction.java:483"); /* skip malformed JSON */ }
            }

            if (totalEntries == 0) {
                sb.append("<div class='empty'>Belum ada riwayat CQI untuk CPMK ini. "
                        + "Isi melalui Nilai OBE &rarr; Portofolio MK &rarr; Edit CQI.</div>");
            } else {
                sb.append("<table class='rw-t'><thead><tr>");
                sb.append("<th>TA / Smtr</th><th>MK</th><th>Masalah</th><th>Analisis Akar Masalah</th>");
                sb.append("<th>Rencana Perbaikan</th><th>PJ</th><th>Target Waktu</th><th>Status</th>");
                sb.append("</tr></thead><tbody>").append(rows2).append("</tbody></table>");
                sb.append("<div style='font-size:9pt;color:#64748b;margin-top:6px'>")
                  .append(totalEntries).append(" entri CQI dari ").append(list.size()).append(" perkuliahan.</div>");
            }
            sb.append("</div>");

        } finally {
            try { sess.close(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/obe/CapaianPembelajaranLulusanAction.java:500"); /* ignore */ }
        }

        MyWindow win = new MyWindow("Riwayat CQI - " + kode, "normal", true);
        win.setWidth("92%");
        win.setHeight("85%");
        org.zkoss.zul.Div d = new org.zkoss.zul.Div();
        d.setStyle("overflow:auto;height:100%;padding:2px");
        d.setParent(win);
        new org.zkoss.zul.Html(sb.toString()).setParent(d);
        // FIX SuspendNotAllowedException "Not attached": win belum di-attach ke root halaman
        // saat dipanggil dari tombol di dalam window modal lain. Pola sama dipakai di
        // sibling class (mis. CapaianLulusanVsBahanKajianAction) -- bungkus onModal() dengan
        // try-catch agar kegagalan attach tidak melempar exception mentah ke user.
        try {
            win.onModal();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private static String escCpmk(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
