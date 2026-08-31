package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Akreditasi;
import ais.database.model.DokumenAkreditasi;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Layar pengelolaan "ruang arsip" akreditasi/dokumen pendukung ({@link Akreditasi}) — folder induk
 * yang menampung berkas {@link DokumenAkreditasi} (dibuka via {@link DokumenAkreditasiAction}
 * tertanam pada baris yang diperluas) untuk keperluan akreditasi program studi/institusi maupun
 * portofolio dosen. Dapat dibuka dalam dua konteks: umum (folder milik satuan kerja/jurusan) atau
 * spesifik satu dosen (parameter request {@code dosen}) — pada konteks dosen, enam folder baku
 * (Thesis, Reviewer publikasi jurnal, SK Jafung/Inpassing, Reviewer Buku/Prosiding, Reviewer HAKI)
 * otomatis dibuat sekali bila dosen belum punya folder sama sekali ({@link #initDefaultFolderDosen()}).
 * Cakupan data dibatasi berlapis: satuan kerja (termasuk turunannya via {@link SatuanKerjaTreeModel})
 * dan role pengguna (kolom {@code kodeGrupPengguna}, kosong berarti terbuka untuk semua role
 * berwenang). Menyediakan juga integrasi ekspor metadata dokumen ke repositori DSpace
 * ({@link #getDspace}/{@link #getDspaceDokumenAkreditasi}, dipakai statis oleh kelas lain seperti
 * {@link DokumenAkreditasiAction}) — token otentikasi DSpace ({@code cookie}) SELALU diteruskan dari
 * pemanggil, tidak ada kredensial yang tertanam di kelas ini. Mengimplementasikan
 * {@link DataCriteria}, {@link DataSearchDefault}, dan {@link DataInitDefault} untuk kompatibilitas
 * dengan komponen baku (paging, cetak/unggah).
 */
public class AkreditasiAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

    private static final long serialVersionUID = -5779730267402400328L;

    private MyWindow addWindow;
    private Paging paging;
    private MyGrid grid;
    private Textbox searchnama;
    private Combobox searchfakultas;
    private Combobox searchjurusan;
    private AmbilDataSatuanKerjaBanbox searchparent;

    private Textbox nama;
    private Textbox keterangan;
    private Combobox jenis;
    private Textbox lembaga;
    private Combobox lingkup;
    private Combobox tingkat;
    private Textbox masaberlaku;
    private MyDatebox mulai;
    private MyDatebox sampai;
    private Intbox tahun;
    private Textbox opini;
    private Combobox jurusan;
    private Textbox peringkat;
    private Textbox kodeGrupPengguna;
    private AmbilDataSatuanKerjaBanbox satuanKerja;

    private boolean edit;
    private boolean delete;
    private Akreditasi akreditasi;
    private MyToolbarbuttonConfig add;
    private Dosen selectedDosen;
    private Tbmuser tbmuser;
    private SatuanKerjaTreeModel satuanKerjaTreeModel;
    private Map<Long, Integer> jumlahDokumenPerFolder = new HashMap<Long, Integer>();

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, Component parent,
            org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    /** Menginisialisasi layar: konteks dosen (bila ada param request {@code dosen}), folder baku dosen, hak akses, toolbar cetak/unggah, listener pencarian, pencarian awal, dan paging. */
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        tbmuser = Common.getCurrentUser();
        satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
        initSelectedDosen();
        initDefaultFolderDosen();
        Common.initFakultasDanJurusan(null, null, searchfakultas, searchjurusan);
        if (add != null) {
        add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
        add.setTooltiptext("Buat ruang arsip dokumen utama");
        }
        edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
        initToolbarExportImport();
        initSearchEvents();
        onSearchDefault(null);
        Common.initPaging(paging, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                onSearchDefault(null);
            }
        });
            FilterLanjutHelper.setup(comp);
}

    private void initSelectedDosen() {
        try {
            String dosenId = execution == null ? null : execution.getParameter("dosen");
            if (dosenId != null && !dosenId.trim().isEmpty()) {
                selectedDosen = (Dosen) ConstantValues.ambil(Dosen.class.getName(), Long.parseLong(dosenId.trim()));
            }
        } catch (Exception e) {
            selectedDosen = null;
        }
    }

    private void initDefaultFolderDosen() {
        if (selectedDosen == null || selectedDosen.getId() == null) {
            return;
        }
        try {
            Session session = HibernateUtil.currentSession();
            Number jumlah = (Number) session.createCriteria(Akreditasi.class)
                    .add(Restrictions.eq("dosen", selectedDosen))
                    .setProjection(Projections.rowCount()).uniqueResult();
            int count = jumlah == null ? 0 : jumlah.intValue();
            if (count > 0) {
                return;
            }
            String[] defaults = new String[] { "Thesis dosen", "Reviewer publikasi jurnal",
                    "SK Jafung Dosen dan SK Inpassing Dosen", "Dok Reviewer Buku", "Dok Reviewer prosiding",
                    "Reviewer HAKI" };
            for (int i = 0; i < defaults.length; i++) {
                Akreditasi data = new Akreditasi();
                data.setDosen(selectedDosen);
                data.setNama(defaults[i]);
                data.setLembaga(Common.getKonfigurasi("label_universitas", "").getNilai());
                data.setLingkup("PT");
                data.setTingkat("Lokal");
                data.setJenis(Akreditasi.DOKUMEN);
                data.setJurusan(selectedDosen.getJurusan());
                data.setAktif(Boolean.TRUE);
                session.save(data);
            }
            session.flush();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void initToolbarExportImport() {
        String[] contents = new String[] { "id", "lembaga", "nama", "lingkup", "tingkat", "masaberlaku",
                "keterangan", "jenis", "mulai", "sampai", "tahun", "opini", "jurusan", "peringkat",
                "kodeGrupPengguna", "satuanKerja" };
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Akreditasi.class, this, contents);
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }
        MyToolbarbuttonConfig upload = Common.uploadData(this, Akreditasi.class, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    private void initSearchEvents() {
        if (searchparent != null) {
            searchparent.setEventListener(new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    onSearchDefault(null);
                }
            });
        }
    }

    /** Renderer baris grid daftar folder akreditasi: baris dapat diperluas untuk membuka {@link DokumenAkreditasiAction} (penjelajah dokumen di dalam folder), info folder (nama dengan link riwayat revisi, jenis/lembaga/lingkup/tingkat/periode/jurusan/jumlah dokumen), checkbox aktif, dan tombol edit/hapus. */
    class AkreditasiRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object data) throws Exception {
            row.setValign("top");
            final Akreditasi folder = (Akreditasi) data;
            final MyDetail detail = new MyDetail();
            detail.setParent(row);
            detail.addEventListener("onOpen", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    Common.clear(detail);
                    if (!detail.isOpen()) {
                        return;
                    }
                    DokumenAkreditasiAction explorer = new DokumenAkreditasiAction(folder, null, false, searchparent);
                    explorer.setParent(detail);
                    explorer.setWidth("100%");
                    explorer.setStyle("border:0; padding:0; margin:0; background:#f8fafc;");
                    explorer.appendChild(new Html(buildDetailTitle(folder)));
                    explorer.init();
                }
            });

            Vbox folderInfo = new Vbox();
            folderInfo.setWidth("100%");
            folderInfo.setParent(row);
            Hbox titleBox = new Hbox();
            titleBox.setWidth("100%");
            titleBox.setAlign("center");
            titleBox.setParent(folderInfo);
            titleBox.appendChild(new Html(buildFolderIconHtml()));
            RevisiHelper.createNewRevisi(Akreditasi.class, folder, safe(folder.getNama())).setParent(titleBox);
            folderInfo.appendChild(new MyLabelAgakKecil(composeFolderSubInfo(folder)));

            new Label(safe(folder.getJenis())).setParent(row);
            new Label(safe(folder.getLembaga())).setParent(row);
            new Label(safe(folder.getLingkup())).setParent(row);
            new Label(safe(folder.getTingkat())).setParent(row);
            new Label(formatPeriode(folder)).setParent(row);
            new Label(folder.getJurusan() == null ? "-" : safe(folder.getJurusan().getNama())).setParent(row);
            new Label(countDokumen(folder) + " dokumen").setParent(row);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(folder.getAktif());
            checkbox.setParent(row);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    folder.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(folder);
                }
            });
            Common.copyEditDeleteButtons(edit, delete, folder, AkreditasiAction.this).setParent(row);
        }
    }

	/**
	 * Menyinkronkan satu {@link DokumenAkreditasi} (dan seluruh anaknya secara rekursif bila
	 * dokumen ini adalah folder, ditandai dengan adanya dokumen berinduk dirinya) ke repositori
	 * DSpace: menyusun metadata Dublin Core (penulis, editor, hak cipta, abstrak, kode, jenis, judul,
	 * subjek, penerbit, URI lampiran bila ada) dan mengirimkannya via
	 * {@link DspaceInformation#dspaceProcess} ke koleksi yang diresolusi lewat
	 * {@link #getDspaceDokumenAkreditasi}, lalu mengunggah file lampiran (bila ada) ke item yang
	 * terbentuk.
	 *
	 * @param tbmuser           pengguna konteks (dipakai sebagai penulis/editor default bila dokumen tidak tertaut dosen)
	 * @param cookie            token sesi/otentikasi DSpace, diteruskan apa adanya dari pemanggil
	 * @param dokumenAkreditasi dokumen (atau folder dokumen) yang disinkronkan
	 * @param update            {@code true} untuk memperbarui item DSpace yang sudah ada, {@code false} untuk membuat baru
	 * @return informasi item DSpace yang terbentuk/diperbarui, atau {@code null} bila dokumen ini adalah folder (anaknya diproses rekursif, bukan dirinya sendiri)
	 * @throws Exception diteruskan apa adanya dari kegagalan panggilan DSpace
	 */
	public static DspaceInformation getDspace(Tbmuser tbmuser, String cookie, DokumenAkreditasi dokumenAkreditasi,
			boolean update) throws Exception {

		Session session = HibernateUtil.currentSession();
		Integer jml = ((Number) session.createCriteria(DokumenAkreditasi.class)
				.add(Restrictions.eq("induk", dokumenAkreditasi)).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (jml > 0) {

			try {
				List<DokumenAkreditasi> dokumenAkreditasis = session.createCriteria(DokumenAkreditasi.class)
						.add(Restrictions.eq("induk", dokumenAkreditasi)).list();

				for (DokumenAkreditasi dokumenAkreditasi2 : dokumenAkreditasis) {
					AkreditasiAction.getDspace(tbmuser, cookie, dokumenAkreditasi2, true);
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			return null;
		}

		JSONArray jsonArray = new JSONArray();

		String nama = tbmuser.getUserNama();
		if (dokumenAkreditasi.getAkreditasi() != null && dokumenAkreditasi.getAkreditasi().getDosen() != null) {
			nama = dokumenAkreditasi.getAkreditasi().getDosen().getNama();
		}

		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.author");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.editor");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", dokumenAkreditasi.getKeterangan());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier");
		jsonMetadata.put("value", dokumenAkreditasi.getKode());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.type");
		jsonMetadata.put("value", dokumenAkreditasi.getAkreditasi().getJenis());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", dokumenAkreditasi.getNama());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.subject");
		jsonMetadata.put("value", dokumenAkreditasi.getInduk() != null ? dokumenAkreditasi.getInduk().getNama()
				: dokumenAkreditasi.getNama());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.publisher");
		jsonMetadata.put("value", Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		LampiranLain lampiranLain = LampiranLain.ambil(dokumenAkreditasi.getId(), DokumenAkreditasi.class.getName());
		if (lampiranLain != null) {
			String uri = lampiranLain.createLinkUri(false);
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);
		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, dokumenAkreditasi,
				jsonPost.toString(), jsonArray.toString(), update, "items",
				"collections/" + getDspaceDokumenAkreditasi(cookie, dokumenAkreditasi) + "/items",
				"items/{uuid}/metadata");

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"File " + dokumenAkreditasi.getNama());

		}

		return dspaceInformation;
	}

	/**
	 * Meresolusi (membuat bila belum ada, kunci UUID disimpan di konfigurasi
	 * {@code dspace_label_collection_dokumenAkreditasi_...}) koleksi DSpace tujuan untuk satu
	 * {@link Akreditasi}: bila akreditasi tertaut jurusan, koleksi dibuat di dalam komunitas jurusan
	 * ({@link JurusanAction#getDspace}); jika tidak, di dalam komunitas perguruan tinggi
	 * ({@link PerguruanTinggiAction#getDspace}).
	 *
	 * @param cookie            token sesi/otentikasi DSpace, diteruskan apa adanya dari pemanggil
	 * @param dokumenAkreditasi dokumen yang akreditasi induknya menentukan koleksi tujuan
	 * @return informasi koleksi DSpace yang terbentuk/sudah ada
	 * @throws Exception diteruskan apa adanya dari kegagalan panggilan DSpace
	 */
	public static DspaceInformation getDspaceDokumenAkreditasi(String cookie, DokumenAkreditasi dokumenAkreditasi)
			throws Exception {

		Akreditasi akreditasi = dokumenAkreditasi.getAkreditasi();

		Jurusan jurusan = akreditasi.getJurusan();
		if (jurusan != null) {
			String description = akreditasi.getNama() + " untuk " + Common.getBahasaConfig("Jurusan") + " "
					+ jurusan.getNama();

			JSONObject jsonPost = new JSONObject();
			jsonPost.put("name", akreditasi.getNama());
			jsonPost.put("copyrightText",
					"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
			jsonPost.put("introductoryText", description);
			jsonPost.put("shortDescription", description);
			jsonPost.put("sidebarText", description);

			Konfigurasi uuidKonfigurasi = Common.getKonfigurasi(
					"dspace_label_collection_dokumenAkreditasi_" + akreditasi.getId() + "_" + jurusan.getId(), "");
			return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
					"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");
		} else {
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

			String description = akreditasi.getNama() + " untuk perguruan tinggi " + perguruanTinggi.getNama();

			JSONObject jsonPost = new JSONObject();
			jsonPost.put("name", akreditasi.getNama());
			jsonPost.put("copyrightText",
					"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
			jsonPost.put("introductoryText", description);
			jsonPost.put("shortDescription", description);
			jsonPost.put("sidebarText", description);

			Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_dokumenAkreditasi_"
					+ akreditasi.getId() + "_perguruanTinggi_" + perguruanTinggi.getId(), "");
			return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
					"communities/" + PerguruanTinggiAction.getDspace(cookie, perguruanTinggi, false) + "/collections");
		}
	}

    /** Membuka form tambah folder akreditasi baru. */
    public void onAdd(Event event) throws Exception {
        init(new Akreditasi());
        addWindow.setVisible(true);
        addWindow.onModal();
    }

    /** Membangun form tambah/ubah folder akreditasi (jenis, lembaga, nama, lingkup, tingkat, periode, jurusan, peringkat, keterangan, kode grup pengguna, satuan kerja) pada jendela dialog. */
    @Override
    public void init(GeneralValueObject obj) throws Exception {
        akreditasi = (Akreditasi) obj;
        init(akreditasi);
        addWindow.setVisible(true);
        addWindow.onModal();
    }

    private void init(Akreditasi data) throws Exception {
        this.akreditasi = data;
        addWindow.setTitle(data.getId() == null ? "Buat Ruang Arsip Dokumen Utama" : "Ubah Ruang Arsip Dokumen Utama");
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
        column.setParent(columns);
        column.setWidth("30%");
        column = new MyColumnConfig();
        column.setParent(columns);

        Rows rows = new Rows();
        rows.setParent(form);
        MyFormRow hero = new MyFormRow();
        ais.ui.util.ZkCompat.setSpans(hero, "2");
        hero.setParent(rows);
        hero.appendChild(new Html(buildFormHeroHtml(data)));

        jenis = createCombo(rows, "Jenis Ruang Arsip *", Akreditasi.jenisDokumenDms(), empty(data.getJenis()) ? Akreditasi.DOKUMEN : data.getJenis());
        if (selectedDosen != null) {
            jenis.setDisabled(true);
        }
        lembaga = createTextbox(rows, "Lembaga / Penerbit *", data.getLembaga(), 2);
        nama = createTextbox(rows, "Nama Ruang Arsip / Dokumen Induk *", data.getNama(), 2);
        lingkup = createCombo(rows, "Lingkup *", Akreditasi.LINGKUP, empty(data.getLingkup()) ? "PT" : data.getLingkup());
        tingkat = createCombo(rows, "Tingkat *", Akreditasi.TINGKAT, empty(data.getTingkat()) ? "Lokal" : data.getTingkat());
        masaberlaku = createTextbox(rows, "Masa Berlaku", data.getMasaberlaku(), 1);
        mulai = createDatebox(rows, "Tanggal Mulai", data.getMulai());
        sampai = createDatebox(rows, "Tanggal Selesai", data.getSampai());
        tahun = createIntbox(rows, "Tahun", data.getTahun());
        opini = createTextbox(rows, "Opini / Catatan Ringkas", data.getOpini(), 2);
        initJurusan(rows, data);
        peringkat = createTextbox(rows, "Status / Peringkat", data.getPeringkat(), 1);
        initSatuanKerja(rows, data);
        initKodeGrupPengguna(rows, data);
        keterangan = createTextbox(rows, "Keterangan", data.getKeterangan(), 4);

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
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
        save.setParent(toolbar);
        borderlayout.setParent(addWindow);
    }

    private Textbox createTextbox(Rows rows, String label, String value, int rowsCount) {
        Row row = createFormRow(rows, label);
        Textbox textbox = new Textbox(value == null ? "" : value);
        textbox.setWidth("90%");
        textbox.setRows(rowsCount);
        row.appendChild(textbox);
        return textbox;
    }

    private Intbox createIntbox(Rows rows, String label, Integer value) {
        Row row = createFormRow(rows, label);
        Intbox intbox = new Intbox();
        intbox.setValue(value);
        intbox.setWidth("90%");
        row.appendChild(intbox);
        return intbox;
    }

    private MyDatebox createDatebox(Rows rows, String label, java.util.Date value) {
        Row row = createFormRow(rows, label);
        MyDatebox datebox = new MyDatebox(value);
        datebox.setReadonly(true);
        datebox.setWidth("90%");
        row.appendChild(datebox);
        return datebox;
    }

    private Combobox createCombo(Rows rows, String label, List<String> values, String selected) {
        Row row = createFormRow(rows, label);
        Combobox combo = new Combobox();
        combo.setWidth("90%");
        combo.setReadonly(true);
        if (values != null) {
            for (String s : values) {
                MyComboitemConfig item = new MyComboitemConfig(s);
                item.setValue(s);
                combo.appendChild(item);
            }
        }
        Common.selectComboItem(combo, selected);
        row.appendChild(combo);
        return combo;
    }

    private Row createFormRow(Rows rows, String label) {
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig(label));
        return row;
    }

    private void initJurusan(Rows rows, Akreditasi data) {
        Row row = createFormRow(rows, "Program Studi");
        jurusan = new Combobox();
        jurusan.setWidth("90%");
        jurusan.setReadonly(true);
        Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class);
        Common.selectComboItem(true, jurusan, data.getJurusan());
        jurusan.setDisabled(selectedDosen != null);
        row.appendChild(jurusan);
    }

    private void initSatuanKerja(Rows rows, Akreditasi data) throws Exception {
        Row row = createFormRow(rows, "Satuan Kerja / Unit");
        satuanKerja = new AmbilDataSatuanKerjaBanbox();
        satuanKerja.setWidth("90%");
        if (data.getSatuanKerja() != null) {
            satuanKerja.setValue(data.getSatuanKerja().getNama());
            satuanKerja.setAttribute("satuanKerja", data.getSatuanKerja());
        }
        row.appendChild(satuanKerja);
    }

    private void initKodeGrupPengguna(Rows rows, Akreditasi data) {
        Row row = createFormRow(rows, "Grup Pengguna");
        row.setVisible(Common.getApakahAdmin());
        kodeGrupPengguna = new Textbox(data.getKodeGrupPengguna());
        kodeGrupPengguna.setWidth("90%");
        kodeGrupPengguna.setRows(2);
        row.appendChild(kodeGrupPengguna);
        MyFormRow note = new MyFormRow();
        note.setVisible(Common.getApakahAdmin());
        ais.ui.util.ZkCompat.setSpans(note, "2");
        note.setParent(rows);
        note.appendChild(new Html("<div style='padding:8px 10px; border-radius:10px; background:#eff6ff; color:#1e3a8a; font-size:11px;'>Kosongkan agar semua pengguna yang berwenang dapat melihat. Untuk banyak role gunakan koma atau titik koma, contoh: am,dosen,pegawai.</div>"));
    }

    /**
     * Memvalidasi lalu menyimpan data folder akreditasi dari form: menolak bila jenis/lembaga/nama/
     * lingkup/tingkat belum lengkap; jika lolos menyimpan/memperbarui entitas dengan seluruh field
     * form serta dosen konteks (bila layar dibuka untuk dosen tertentu).
     *
     * @param event event ZK pemicu penyimpanan (tombol simpan)
     * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal
     * @throws Exception diteruskan apa adanya dari kegagalan Hibernate saat menyimpan
     */
    public boolean onSave(Event event) throws Exception {
        if (jenis == null || jenis.getSelectedItem() == null) {
            return showWarning("Jenis ruang arsip harus diisi");
        }
        if (lembaga == null || lembaga.getValue().trim().isEmpty()) {
            return showWarning("Lembaga harus diisi");
        }
        if (nama == null || nama.getValue().trim().isEmpty()) {
            return showWarning("Nama ruang arsip/dokumen induk harus diisi");
        }
        if (lingkup == null || lingkup.getSelectedItem() == null) {
            return showWarning("Lingkup harus diisi");
        }
        if (tingkat == null || tingkat.getSelectedItem() == null) {
            return showWarning("Tingkat harus diisi");
        }
        Session session = HibernateUtil.currentSession();
        if (akreditasi.getId() != null) {
            akreditasi = (Akreditasi) session.load(Akreditasi.class, akreditasi.getId());
        }
        akreditasi.setNama(nama.getValue().trim());
        akreditasi.setLembaga(lembaga.getValue().trim());
        akreditasi.setLingkup((String) lingkup.getSelectedItem().getValue());
        akreditasi.setTingkat((String) tingkat.getSelectedItem().getValue());
        akreditasi.setJenis((String) jenis.getSelectedItem().getValue());
        akreditasi.setMasaberlaku(masaberlaku.getValue().trim());
        akreditasi.setMulai(mulai.getValue());
        akreditasi.setSampai(sampai.getValue());
        akreditasi.setTahun(tahun.getValue());
        akreditasi.setOpini(opini.getValue());
        akreditasi.setJurusan((Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue()));
        akreditasi.setPeringkat(peringkat.getValue());
        akreditasi.setKeterangan(keterangan.getValue());
        akreditasi.setDosen(selectedDosen);
        akreditasi.setKodeGrupPengguna(kodeGrupPengguna == null ? "" : kodeGrupPengguna.getValue().trim());
        akreditasi.setSatuanKerja((SatuanKerja) (satuanKerja == null ? null : satuanKerja.getAttribute("satuanKerja")));
        akreditasi.setAktif(akreditasi.getAktif());
        Common.refreshSaveOrUpdate(session, akreditasi);
        return true;
    }

    private boolean showWarning(String message) {
        try {
            MyMessageboxConfig.show(message, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/AkreditasiAction.java:646");
            }
        }
        return false;
    }

    /**
     * Menyusun kriteria pencarian {@link Akreditasi} dengan cakupan berlapis: satuan kerja (folder
     * milik admin tanpa satuan kerja SELALU ikut, sisanya dibatasi ke satuan kerja pengguna dan
     * turunannya, atau ke satu satuan kerja terpilih beserta anaknya via {@code searchparent}), role
     * pengguna (kolom {@code kodeGrupPengguna} kosong berarti terbuka untuk semua), kata kunci
     * (nama/lembaga/keterangan), jurusan, dan konteks dosen ({@link #selectedDosen} bila layar
     * dibuka untuk dosen tertentu, jika tidak hanya folder tanpa dosen).
     *
     * @param order {@code true} untuk menyertakan pengurutan (jenis, nama, id)
     * @return kriteria Hibernate siap dieksekusi
     */
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
        String role = tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleId();
        String keyword = searchnama == null ? "" : searchnama.getValue().trim();
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Akreditasi.class)
                .add(Restrictions.or(
                        !Common.getApakahAdmin() ? Restrictions.sqlRestriction("false") : Restrictions.isNull("satuanKerja"),
                        satuanKerjas == null || satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.or(parent == null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas))))
                .add(role.isEmpty() || Common.getApakahAdmin() ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.or(Restrictions.isNull("kodeGrupPengguna"),
                                Restrictions.or(Restrictions.eq("kodeGrupPengguna", ""), Restrictions.ilike("kodeGrupPengguna", "," + role + ",", MatchMode.ANYWHERE))))
                .add(keyword.isEmpty() ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.or(Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE),
                                Restrictions.or(Restrictions.ilike("lembaga", keyword, MatchMode.ANYWHERE), Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE))))
                .add(searchjurusan == null ? Restrictions.sqlRestriction("1=1") : CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
                .add(selectedDosen == null || selectedDosen.getId() == null ? Restrictions.isNull("dosen") : Restrictions.eq("dosen", selectedDosen));
        if (order) {
            criteria.addOrder(Order.asc("jenis")).addOrder(Order.asc("nama")).addOrder(Order.asc("id"));
        }
        return criteria;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    /** Menjalankan pencarian folder akreditasi sesuai kriteria dan halaman paging aktif, menghitung jumlah dokumen per folder, lalu merender hasilnya ke grid. */
    public void onSearchDefault(Event event) {
        if (grid == null) {
            return;
        }
        try {
            Common.initPaging(initCriteria(false), paging);
            List<Akreditasi> folders = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                    .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
            prepareJumlahDokumenPerFolder(folders);
            ListModel model = new SimpleListModel(folders);
            grid.setRowRenderer(new AkreditasiRenderer());
            grid.setModelCheckMobile(model);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            grid.setModelCheckMobile(new SimpleListModel(new ArrayList()));
        }
    }

    @SuppressWarnings("unchecked")
    private void prepareJumlahDokumenPerFolder(List<Akreditasi> folders) {
        jumlahDokumenPerFolder = new HashMap<Long, Integer>();
        if (folders == null || folders.size() == 0) {
            return;
        }
        List<Long> ids = new ArrayList<Long>();
        for (int i = 0; i < folders.size(); i++) {
            Akreditasi folder = folders.get(i);
            if (folder != null && folder.getId() != null) {
                ids.add(folder.getId());
                jumlahDokumenPerFolder.put(folder.getId(), Integer.valueOf(0));
            }
        }
        if (ids.size() == 0) {
            return;
        }
        try {
            List<Object[]> rows = HibernateUtil.currentSession().createQuery(
                    "select d.akreditasi.id, count(d.id) from DokumenAkreditasi d "
                            + "where d.akreditasi.id in (:ids) group by d.akreditasi.id")
                    .setParameterList("ids", ids).list();
            for (int i = 0; rows != null && i < rows.size(); i++) {
                Object[] row = rows.get(i);
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    Long id = (Long) row[0];
                    Number jumlah = (Number) row[1];
                    jumlahDokumenPerFolder.put(id, Integer.valueOf(jumlah.intValue()));
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private int countDokumen(Akreditasi folder) {
        if (folder == null || folder.getId() == null) {
            return 0;
        }
        Integer cached = jumlahDokumenPerFolder == null ? null : jumlahDokumenPerFolder.get(folder.getId());
        if (cached != null) {
            return cached.intValue();
        }
        try {
            Number n = (Number) HibernateUtil.currentSession().createCriteria(DokumenAkreditasi.class)
                    .add(Restrictions.eq("akreditasi", folder))
                    .setProjection(Projections.rowCount()).uniqueResult();
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return 0;
        }
    }

    private String composeFolderSubInfo(Akreditasi folder) {
        StringBuilder sb = new StringBuilder();
        sb.append(safe(folder.getLembaga()));
        if (folder.getTahun() != null) {
            sb.append(" • ").append(folder.getTahun());
        }
        if (!safe(folder.getPeringkat()).isEmpty()) {
            sb.append(" • ").append(safe(folder.getPeringkat()));
        }
        if (!safe(folder.getKeterangan()).isEmpty()) {
            sb.append(" • ").append(safe(folder.getKeterangan()));
        }
        return sb.toString();
    }

    private String formatPeriode(Akreditasi folder) {
        StringBuilder sb = new StringBuilder();
        if (folder.getMulai() != null) {
            sb.append(Common.dateFormat2.get().format(folder.getMulai()));
        }
        if (folder.getSampai() != null) {
            if (sb.length() > 0) {
                sb.append(" s.d ");
            }
            sb.append(Common.dateFormat2.get().format(folder.getSampai()));
        }
        if (sb.length() == 0) {
            sb.append(safe(folder.getMasaberlaku()));
        }
        return sb.toString();
    }

    private String buildDetailTitle(Akreditasi folder) {
        return "<div style='padding:14px 16px; border-radius:16px; margin:8px; background:linear-gradient(135deg,#eff6ff,#ffffff); border:1px solid #dbeafe; box-shadow:0 10px 24px rgba(15,23,42,.06);'>"
                + "<div style='display:flex; align-items:center; gap:10px;'>"
                + "<span style='display:inline-flex; width:34px; height:34px; align-items:center; justify-content:center; border-radius:12px; background:#dbeafe; color:#1d4ed8;'><i class='fa fa-folder-open'></i></span>"
                + "<div><div style='font-size:18px; font-weight:800; color:#0f172a;'>" + html(folder.getNama()) + "</div>"
                + "<div style='font-size:11.5px; color:#64748b; margin-top:4px;'>Ruang arsip ini menampilkan sub ruang dan file dokumen yang tersusun rapi agar mudah dicari, diperiksa, dan diperbarui.</div></div></div></div>";
    }

    private String buildFormHeroHtml(Akreditasi data) {
        return "<div style='padding:14px 16px; border-radius:16px; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:white; margin-bottom:8px; box-shadow:0 10px 24px rgba(15,23,42,.16);'>"
                + "<div style='font-size:18px; font-weight:800;'><i class='fa fa-folder-open' style='margin-right:8px;'></i>" + (data.getId() == null ? "Buat Ruang Arsip Dokumen" : "Ubah Ruang Arsip Dokumen") + "</div>"
                + "<div style='font-size:12px; opacity:.92; margin-top:4px;'>Ruang arsip utama berfungsi sebagai induk pengelompokan dokumen perguruan tinggi, yayasan, unit, fakultas, atau program studi.</div></div>";
    }

    private String buildFolderIconHtml() {
        return "<span style='display:inline-flex; width:34px; height:34px; align-items:center; justify-content:center; border-radius:12px; background:#eff6ff; color:#1d4ed8; margin-right:8px; font-size:16px;'><i class='fa fa-folder-open'></i></span>";
    }

    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String html(String value) {
        String s = safe(value);
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
