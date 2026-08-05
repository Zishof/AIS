package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class JenisItemPenilaianSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
//	private Textbox searchketerangan;
	private Combobox searchKategori;
	private Combobox searchtipe;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Checkbox searchaktif;

	private MyCheckboxConfig harusMenyertakanLampiran;

	private Combobox tipeDataInputan;
	private Textbox labelInputan;
	private Textbox keterangan;

	private boolean edit = true;
	private boolean delete = true;

	private JenisItemPenilaianSiswa jenisItemPenilaianSiswa;
	private Textbox nilaiDataInputan;

	private MyToolbarbuttonConfig find;
	private MyCheckboxConfig tampilkanIsianKeterangan;
	private MyTextbox labelInputanKeterangan;
	private MyIntbox jumlahBaris;
	private Textbox kode;
	private Combobox kategoriItemPenilaianSiswa;
	private Combobox yayasan;
	private Combobox sekolah;
	private Row rowFormula;
	private JSONArray array;
	private Row rowIsianFormula;
	private MyDoublebox nilaiMax;
	private MyDoublebox nilaiMin;
	private Row rowFormula1;
	private MyCheckboxConfig hitungRataRataKelas;
	private MyCheckboxConfig hitungRataRataAngkatan;
	private MyIntbox jumlahText;
	public static String[] contents = new String[] { "id", "kode", "nama", "kategoriItemPenilaianSiswa", "labelInputan",
			"tipeDataInputan", "nilaiDataInputan", "harusMenyertakanLampiran", "aktif", "nomorUrut", "wajibDiisi",
			"keterangan", "tampilkanIsianKeterangan", "labelInputanKeterangan", "formula", "yayasan", "sekolah",
			"hitungRataRataKelas", "hitungRataRataAngkatan" };

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);
		if (searchtipe != null) { searchtipe.setWidth("90%"); }
		if (searchtipe != null) { searchtipe.setReadonly(true); }
		MyComboitemConfig comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.TIDAK_ADA);
		if (comboitem != null) { comboitem.setValue(JenisItemPenilaianSiswa.TIDAK_ADA); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.TEXT);
		if (comboitem != null) { comboitem.setValue(JenisItemPenilaianSiswa.TEXT); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.ANGKA);
		if (comboitem != null) { comboitem.setValue(JenisItemPenilaianSiswa.ANGKA); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.TEXT_ANGKA);
		if (comboitem != null) { comboitem.setValue(JenisItemPenilaianSiswa.TEXT_ANGKA); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.TANGGAL);
		if (comboitem != null) { comboitem.setValue(JenisItemPenilaianSiswa.TANGGAL); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.PILIHAN_YA_TIDAK);
		if (comboitem != null) { comboitem.setValue(JenisItemPenilaianSiswa.PILIHAN_YA_TIDAK); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.PILIHAN_CUSTOM);
		if (comboitem != null) { comboitem.setValue(JenisItemPenilaianSiswa.PILIHAN_CUSTOM); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.PILIHAN_BANYAK);
		if (comboitem != null) { comboitem.setValue(JenisItemPenilaianSiswa.PILIHAN_BANYAK); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.FORMULA);
		if (comboitem != null) { comboitem.setValue(JenisItemPenilaianSiswa.FORMULA); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchtipe.appendChild(comboitem);
		if (searchtipe != null) { searchtipe.setSelectedItem(comboitem); }
		if (searchtipe != null) { searchtipe.setReadonly(true); }

		EventListener kategoriSekolah = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah mySekolah = (Sekolah) (searchsekolah.getSelectedItem() == null ? null
						: searchsekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(searchKategori, new String[] { "kode", "nama" }, "keterangan",
						KategoriItemPenilaianSiswa.class, "=Semua Kategori=",
						mySekolah == null ? Restrictions.eq("aktif", true)
								: Restrictions.and(Restrictions.eq("sekolah", mySekolah),
										Restrictions.eq("aktif", true)));

			}
		};
		searchsekolah.addEventListener("onChange", kategoriSekolah);
		Common.createDefaultTimer(kategoriSekolah);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisItemPenilaianSiswa.class, contents);
		if (upload != null) { upload.setVisible(edit && delete); }
		Common.appendKeToolbar(upload, find, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class JenisItemPenilaianSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisItemPenilaianSiswa jenisItemPenilaianSiswa = (JenisItemPenilaianSiswa) arg1;
			new MyLabelAgakKecil(jenisItemPenilaianSiswa.getKode() + "-" + jenisItemPenilaianSiswa.getLabelInputan())
					.setParent(arg0);
			new Label(jenisItemPenilaianSiswa.getKategoriItemPenilaianSiswa() == null ? ""
					: jenisItemPenilaianSiswa.getKategoriItemPenilaianSiswa().getNama()).setParent(arg0);
			new Label(jenisItemPenilaianSiswa.getHarusMenyertakanLampiran() ? "Ya" : "Tidak").setParent(arg0);

			RevisiHelper.createNewRevisi(JenisItemPenilaianSiswa.class, jenisItemPenilaianSiswa,
					jenisItemPenilaianSiswa.getTipeDataInputan()).setParent(arg0);
			new MyLabelAgakKecil(jenisItemPenilaianSiswa.getNilaiDataInputan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisItemPenilaianSiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisItemPenilaianSiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisItemPenilaianSiswa);
				}
			});

			final MyCheckboxConfig wajib = new MyCheckboxConfig("Isian Wajib");
			wajib.setChecked(jenisItemPenilaianSiswa.getWajibDiisi());
			wajib.setParent(arg0);
			wajib.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisItemPenilaianSiswa.setWajibDiisi(wajib.isChecked());
					Common.refreshSaveOrUpdate(jenisItemPenilaianSiswa);
				}
			});

			if (jenisItemPenilaianSiswa.getHarusMenyertakanLampiran()) {
				final MyCheckboxConfig lampiranWajib = new MyCheckboxConfig("Lampiran Wajib");
				lampiranWajib.setChecked(jenisItemPenilaianSiswa.getLampiranWajibDiisi());
				lampiranWajib.setParent(arg0);
				lampiranWajib.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						jenisItemPenilaianSiswa.setLampiranWajibDiisi(lampiranWajib.isChecked());
						Common.refreshSaveOrUpdate(jenisItemPenilaianSiswa);
					}
				});
			} else {
				new Label().setParent(arg0);
			}

			final Intbox intbox = new Intbox(jenisItemPenilaianSiswa.getNomorUrut());
			intbox.setWidth("90%");
			intbox.setParent(arg0);
			intbox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisItemPenilaianSiswa.setNomorUrut(intbox.getValue());
					Common.refreshSaveOrUpdate(jenisItemPenilaianSiswa);
				}
			});
			final MyTextbox kodeAdminYgBoleh = new MyTextbox(jenisItemPenilaianSiswa.getKodeAdminYgBoleh());
			final MyLabelKecil label = new MyLabelKecil(
					"Masukkan kode admin yg boleh ubah, jika lebih dari satu pisahkan dengan tanda koma");

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			final MyCheckboxConfig hanyaTampilDiAdmin = new MyCheckboxConfig("Hanya Admin");
			hanyaTampilDiAdmin.setChecked(jenisItemPenilaianSiswa.getHanyaTampilDiAdmin());
			hanyaTampilDiAdmin.setParent(vbox);
			hanyaTampilDiAdmin.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisItemPenilaianSiswa.setHanyaTampilDiAdmin(hanyaTampilDiAdmin.isChecked());
					Common.refreshSaveOrUpdate(jenisItemPenilaianSiswa);
					label.setVisible(jenisItemPenilaianSiswa.getHanyaTampilDiAdmin());
					kodeAdminYgBoleh.setVisible(jenisItemPenilaianSiswa.getHanyaTampilDiAdmin());
				}
			});
			label.setParent(vbox);
			kodeAdminYgBoleh.setParent(vbox);

			kodeAdminYgBoleh.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisItemPenilaianSiswa.setKodeAdminYgBoleh(kodeAdminYgBoleh.getValue().trim());
					Common.refreshSaveOrUpdate(jenisItemPenilaianSiswa);
				}
			});

			label.setVisible(jenisItemPenilaianSiswa.getHanyaTampilDiAdmin());
			kodeAdminYgBoleh.setVisible(jenisItemPenilaianSiswa.getHanyaTampilDiAdmin());

			new MyLabelAgakKecil(jenisItemPenilaianSiswa.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, jenisItemPenilaianSiswa, JenisItemPenilaianSiswaAction.this)
					.setParent(arg0);

		}

	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisItemPenilaianSiswa = (JenisItemPenilaianSiswa) obj;
		init(jenisItemPenilaianSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new JenisItemPenilaianSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final JenisItemPenilaianSiswa jenisItemPenilaianSiswa) throws Exception {
		this.jenisItemPenilaianSiswa = jenisItemPenilaianSiswa;
		addWindow.setTitle(jenisItemPenilaianSiswa.getId() == null ? "Tambah Item Penilaian" : "Ubah Item Penilaian");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Item Penilaian *"));
		row.appendChild(kode = new Textbox(jenisItemPenilaianSiswa.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Item Penilaian *"));
		row.appendChild(labelInputan = new Textbox(jenisItemPenilaianSiswa.getLabelInputan()));
		labelInputan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Menyertakan file lampiran"));
		row.appendChild(harusMenyertakanLampiran = new MyCheckboxConfig());
		harusMenyertakanLampiran.setChecked(jenisItemPenilaianSiswa.getHarusMenyertakanLampiran());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampilkan isian keterangan"));
		row.appendChild(tampilkanIsianKeterangan = new MyCheckboxConfig());
		tampilkanIsianKeterangan.setChecked(jenisItemPenilaianSiswa.getTampilkanIsianKeterangan());

		final MyFormRow rowlabelInputanKeterangan = new MyFormRow();
		rowlabelInputanKeterangan.setStyle("border:0px;background: transparent;");
		rowlabelInputanKeterangan.setVisible(tampilkanIsianKeterangan.isChecked());
		rowlabelInputanKeterangan.setParent(rows);
		rowlabelInputanKeterangan.appendChild(new ais.ui.util.MyLabelConfig("Label isian keterangan"));
		rowlabelInputanKeterangan.appendChild(
				labelInputanKeterangan = new MyTextbox(jenisItemPenilaianSiswa.getLabelInputanKeterangan()));
		labelInputanKeterangan.setWidth("90%");
		tampilkanIsianKeterangan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowlabelInputanKeterangan.setVisible(tampilkanIsianKeterangan.isChecked());
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Data Inputan"));
		row.appendChild(tipeDataInputan = new Combobox());
		tipeDataInputan.setWidth("90%");
		tipeDataInputan.setReadonly(true);
		MyComboitemConfig comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.TIDAK_ADA);
		comboitem.setValue(JenisItemPenilaianSiswa.TIDAK_ADA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.TEXT);
		comboitem.setValue(JenisItemPenilaianSiswa.TEXT);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.ANGKA);
		comboitem.setValue(JenisItemPenilaianSiswa.ANGKA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.TEXT_ANGKA);
		comboitem.setValue(JenisItemPenilaianSiswa.TEXT_ANGKA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.TANGGAL);
		comboitem.setValue(JenisItemPenilaianSiswa.TANGGAL);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.PILIHAN_YA_TIDAK);
		comboitem.setValue(JenisItemPenilaianSiswa.PILIHAN_YA_TIDAK);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.PILIHAN_CUSTOM);
		comboitem.setValue(JenisItemPenilaianSiswa.PILIHAN_CUSTOM);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.PILIHAN_BANYAK);
		comboitem.setValue(JenisItemPenilaianSiswa.PILIHAN_BANYAK);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JenisItemPenilaianSiswa.FORMULA);
		comboitem.setValue(JenisItemPenilaianSiswa.FORMULA);
		tipeDataInputan.appendChild(comboitem);

		Common.selectComboItem(tipeDataInputan, jenisItemPenilaianSiswa.getTipeDataInputan());
		if (tipeDataInputan.getSelectedItem() == null) {
			tipeDataInputan.setSelectedIndex(0);
		}
		tipeDataInputan.setReadonly(true);

		final MyFormRow rowNilaiInputan = new MyFormRow();
		rowNilaiInputan.setStyle("border:0px;background: transparent;");
		rowNilaiInputan.setParent(rows);
		rowNilaiInputan.appendChild(new ais.ui.util.MyLabelConfig("Nilai Data Inputan"));
		rowNilaiInputan.appendChild(nilaiDataInputan = new Textbox(jenisItemPenilaianSiswa.getNilaiDataInputan()));
		nilaiDataInputan.setWidth("90%");
		nilaiDataInputan.setRows(3);

		final Row rowKeteranganNilaiInputan = Common.initKeterangan(rows,
				"Nilai inputan ini berfungsi hanya jika tipe inputan-nya berupa pilihan custom. Input nilai custom harus diberi pemisah semicolon (;) dan untuk skor dipisah dengan kolon (:), skor harus berupa angka desimal, contoh : Ya:1;Tidak:0;Belum Tau:2");

		final MyFormRow rowJumlahBaris = new MyFormRow();
		rowJumlahBaris.setStyle("border:0px;background: transparent;");
		rowJumlahBaris.setParent(rows);
		rowJumlahBaris.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Baris"));
		rowJumlahBaris.appendChild(jumlahBaris = new MyIntbox(jenisItemPenilaianSiswa.getJumlahBaris()));
		
		
		final MyFormRow rowJumlahText = new MyFormRow();
		rowJumlahText.setStyle("border:0px;background: transparent;");
		rowJumlahText.setParent(rows);
		rowJumlahText.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Maksimal Teks"));
		rowJumlahText.appendChild(jumlahText = new MyIntbox(jenisItemPenilaianSiswa.getJumlahText()));

		final MyFormRow rowNilaiMax = new MyFormRow();
		rowNilaiMax.setStyle("border:0px;background: transparent;");
		rowNilaiMax.setParent(rows);
		rowNilaiMax.appendChild(new ais.ui.util.MyLabelConfig("Nilai Maksimal"));
		rowNilaiMax.appendChild(nilaiMax = new MyDoublebox(jenisItemPenilaianSiswa.getNilaiMax()));

		final MyFormRow rowNilaiMin = new MyFormRow();
		rowNilaiMin.setStyle("border:0px;background: transparent;");
		rowNilaiMin.setParent(rows);
		rowNilaiMin.appendChild(new ais.ui.util.MyLabelConfig("Nilai Minimal"));
		rowNilaiMin.appendChild(nilaiMin = new MyDoublebox(jenisItemPenilaianSiswa.getNilaiMin()));

		rowFormula1 = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowFormula1, "2");
		rowFormula1.setParent(rows);
		rowFormula1.appendChild(new ais.ui.util.MyLabelConfig("Formula"));

		rowIsianFormula = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowIsianFormula, "2");
		rowIsianFormula.setParent(rows);
		array = new JSONArray(jenisItemPenilaianSiswa.getFormula());
		rowFormula = Common.tampilanScroll1(rowIsianFormula);
		HashMap<Long, DetailGrupPenilaian> selectedGrupKategoriItemPenilaianSiswa = null;
		ArrayList<EventListener> eventListeners = null;
		GrupPenilaianAction.reloadFormula(rowFormula, null, array, null, selectedGrupKategoriItemPenilaianSiswa,
				eventListeners);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String v = (String) (tipeDataInputan.getSelectedItem() == null ? null
						: tipeDataInputan.getSelectedItem().getValue());

				rowNilaiMax.setVisible(v != null && (v.equals(ParameterTambahan.ANGKA)));
				rowNilaiMin.setVisible(v != null && (v.equals(ParameterTambahan.ANGKA)));

				rowNilaiInputan.setVisible(v != null && (v.equals(JenisItemPenilaianSiswa.PILIHAN_CUSTOM)
						|| v.equals(JenisItemPenilaianSiswa.PILIHAN_BANYAK)));
				rowKeteranganNilaiInputan.setVisible(v != null && (v.equals(JenisItemPenilaianSiswa.PILIHAN_CUSTOM)
						|| v.equals(JenisItemPenilaianSiswa.PILIHAN_BANYAK)));

				rowJumlahBaris.setVisible(v != null && v.equals(JenisItemPenilaianSiswa.TEXT));

				rowFormula.setVisible(v != null && v.equals(JenisItemPenilaianSiswa.FORMULA));
				rowIsianFormula.setVisible(v != null && v.equals(JenisItemPenilaianSiswa.FORMULA));
				rowFormula1.setVisible(v != null && v.equals(JenisItemPenilaianSiswa.FORMULA));
				rowJumlahText.setVisible(v != null && v.equals(ParameterTambahan.TEXT));
			}
		};
		eventListener.onEvent(null);
		tipeDataInputan.addEventListener("onChange", eventListener);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, jenisItemPenilaianSiswa.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, jenisItemPenilaianSiswa.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori Penilaian"));
		row.appendChild(kategoriItemPenilaianSiswa = new Combobox());
		kategoriItemPenilaianSiswa.setWidth("90%");

		EventListener kategoriSekolah = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null ? null
						: sekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(kategoriItemPenilaianSiswa, new String[] { "kode", "nama" }, "keterangan",
						KategoriItemPenilaianSiswa.class, "=Tanpa Kategori=",
						mySekolah == null ? Restrictions.eq("aktif", true)
								: Restrictions.and(Restrictions.eq("sekolah", mySekolah),
										Restrictions.eq("aktif", true)));
				Common.selectComboItem(kategoriItemPenilaianSiswa,
						jenisItemPenilaianSiswa.getKategoriItemPenilaianSiswa());

			}
		};
		sekolah.addEventListener("onChange", kategoriSekolah);
		Common.createDefaultTimer(kategoriSekolah);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(hitungRataRataKelas = new MyCheckboxConfig("Hitung rata-rata nilai siswa dalam satu kelas"));
		hitungRataRataKelas.setChecked(jenisItemPenilaianSiswa.getHitungRataRataKelas());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				hitungRataRataAngkatan = new MyCheckboxConfig("Hitung rata-rata nilai siswa dalam satu angkatan"));
		hitungRataRataAngkatan.setChecked(jenisItemPenilaianSiswa.getHitungRataRataAngkatan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				jenisItemPenilaianSiswa.getKeterangan() == null ? "" : jenisItemPenilaianSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
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

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (labelInputan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		Session session = HibernateUtil.currentSession();
		if (jenisItemPenilaianSiswa.getId() != null) {
			jenisItemPenilaianSiswa = (JenisItemPenilaianSiswa) session.load(JenisItemPenilaianSiswa.class,
					jenisItemPenilaianSiswa.getId());

		}
		jenisItemPenilaianSiswa.setKategoriItemPenilaianSiswa(
				(KategoriItemPenilaianSiswa) (kategoriItemPenilaianSiswa.getSelectedItem() == null ? null
						: kategoriItemPenilaianSiswa.getSelectedItem().getValue()));
		jenisItemPenilaianSiswa.setKode(kode.getValue().trim());
		jenisItemPenilaianSiswa.setNilaiDataInputan(nilaiDataInputan.getValue().trim());
		jenisItemPenilaianSiswa.setNama(labelInputan.getValue().trim());
		jenisItemPenilaianSiswa.setHarusMenyertakanLampiran(harusMenyertakanLampiran.isChecked());
		jenisItemPenilaianSiswa.setTipeDataInputan((String) tipeDataInputan.getSelectedItem().getValue());
		jenisItemPenilaianSiswa.setLabelInputan(labelInputan.getValue().trim());
		jenisItemPenilaianSiswa.setKeterangan(keterangan.getValue());

		jenisItemPenilaianSiswa.setTampilkanIsianKeterangan(tampilkanIsianKeterangan.isChecked());
		jenisItemPenilaianSiswa.setLabelInputanKeterangan(labelInputanKeterangan.getValue());
		jenisItemPenilaianSiswa.setJumlahBaris(jumlahBaris.getValue());

		jenisItemPenilaianSiswa.setNilaiMax(nilaiMax.getValue());
		jenisItemPenilaianSiswa.setNilaiMin(nilaiMin.getValue());

		jenisItemPenilaianSiswa.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		jenisItemPenilaianSiswa.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());

		jenisItemPenilaianSiswa.setFormula(array.toString());

		jenisItemPenilaianSiswa.setHitungRataRataAngkatan(hitungRataRataAngkatan.isChecked());
		jenisItemPenilaianSiswa.setHitungRataRataKelas(hitungRataRataKelas.isChecked());
		jenisItemPenilaianSiswa.setJumlahText(jumlahText.getValue());
		Common.refreshSaveOrUpdate(session, jenisItemPenilaianSiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisItemPenilaianSiswa.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nomorUrut"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

//				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
//						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchKategori.getSelectedItem() == null || searchKategori.getSelectedItem().getValue() == null
						|| searchKategori.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kategoriItemPenilaianSiswa",
										searchKategori.getSelectedItem().getValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sekolah"),
										CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false)))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("yayasan"),
										CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)))

				.add(searchtipe.getSelectedItem() == null || searchtipe.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tipeDataInputan", searchtipe.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisItemPenilaianSiswa);
		grid.setRowRenderer(new JenisItemPenilaianSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
