package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.util.GrupPenilaianUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JenisNilaiHuruf;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk grup penilaian. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchyayasan}, {@code Combobox
 * searchsekolah}, {@code Checkbox searchaktif}, {@code Textbox nama}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code reloadDataFormula()}, {@code reloadFormula()}, {@code onSearchDefault()}); mutasi
 * data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class GrupPenilaianAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Checkbox searchaktif;

	private Textbox nama;
	private Combobox sekolah;
	private Textbox keterangan;
	private boolean edit = false;
	private boolean delete = false;

	private GrupPenilaian grupPenilaian;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;

	private HashMap<Long, DetailGrupPenilaian> selectedGrupKategoriItemPenilaianSiswa;
	private List<EventListener> eventListeners;

	private JSONArray array;
	private Row rowFormula;
	private Row rowJp;
	private Combobox jenisNilaiHuruf;
	private Combobox khususTingkat;
	private Combobox khususSemester;

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

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "jenis", "formula", "sekolah", "adaTotal", "jenisNilaiHuruf",
				"nilaiBolehDinputOlehGuru", "khususSemester", "khususTingkat", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, GrupPenilaian.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class GrupPenilaianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GrupPenilaian grupPenilaian = (GrupPenilaian) arg1;

			RevisiHelper.createNewRevisi(GrupPenilaian.class, grupPenilaian, grupPenilaian.getNama()).setParent(arg0);
			new Label(grupPenilaian.getSekolah() == null ? "" : grupPenilaian.getSekolah().getNama()).setParent(arg0);
			new Label(grupPenilaian.getJenisNilaiHuruf() == null ? "" : grupPenilaian.getJenisNilaiHuruf().getNama())
					.setParent(arg0);
			new Label(grupPenilaian.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(grupPenilaian.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					grupPenilaian.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(grupPenilaian);
				}
			});

			final MyCheckboxConfig nilaiBolehDinputOlehGuru = new MyCheckboxConfig("Boleh Diinput Guru");
			nilaiBolehDinputOlehGuru.setDisabled(!edit);
			nilaiBolehDinputOlehGuru.setChecked(grupPenilaian.getNilaiBolehDinputOlehGuru());
			nilaiBolehDinputOlehGuru.setParent(arg0);
			nilaiBolehDinputOlehGuru.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					grupPenilaian.setNilaiBolehDinputOlehGuru(nilaiBolehDinputOlehGuru.isChecked());
					Common.refreshSaveOrUpdate(grupPenilaian);
				}
			});

			final MyCheckboxConfig tampilDirekap = new MyCheckboxConfig("Tampil di rekap");
			tampilDirekap.setDisabled(!edit);
			tampilDirekap.setChecked(grupPenilaian.getTampilDirekap());
			tampilDirekap.setParent(arg0);
			tampilDirekap.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					grupPenilaian.setTampilDirekap(tampilDirekap.isChecked());
					Common.refreshSaveOrUpdate(grupPenilaian);
				}
			});

			final MyCheckboxConfig adaTotal = new MyCheckboxConfig("Ada Total");
			adaTotal.setDisabled(!edit);
			adaTotal.setChecked(grupPenilaian.getAdaTotal());
			adaTotal.setParent(arg0);
			adaTotal.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					grupPenilaian.setAdaTotal(adaTotal.isChecked());
					Common.refreshSaveOrUpdate(grupPenilaian);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, grupPenilaian, GrupPenilaianAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new GrupPenilaian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		grupPenilaian = (GrupPenilaian) obj;
		init(grupPenilaian);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(final GrupPenilaian grupPenilaian) throws Exception {
		this.grupPenilaian = grupPenilaian;
		addWindow.setTitle(grupPenilaian.getId() == null ? "Tambah Jenis Penilaian" : "Ubah Jenis Penilaian");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Penilaian *"));
		row.appendChild(nama = new Textbox(grupPenilaian.getNama()));
		nama.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, grupPenilaian.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, grupPenilaian.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus buat tingkat"));
		row.appendChild(khususTingkat = new Combobox());
		khususTingkat.setWidth("90%");

		Comboitem comboitem = new Comboitem("Semua Tingkat");
		comboitem.setValue(null);
		khususTingkat.appendChild(comboitem);

		for (int i = 1; i <= 12; i++) {
			comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			khususTingkat.appendChild(comboitem);
		}

		Common.selectComboItem(khususTingkat, grupPenilaian.getKhususTingkat());
		khususTingkat.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus buat Semester"));
		row.appendChild(khususSemester = new Combobox());
		khususSemester.setWidth("90%");

		comboitem = new Comboitem("Semua Semester");
		comboitem.setValue(null);
		khususSemester.appendChild(comboitem);

		for (int i = 1; i <= 2; i++) {
			comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			khususSemester.appendChild(comboitem);
		}

		Common.selectComboItem(khususSemester, grupPenilaian.getKhususSemester());
		khususSemester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Nilai Huruf"));
		row.appendChild(jenisNilaiHuruf = new Combobox());
		jenisNilaiHuruf.setWidth("90%");
		jenisNilaiHuruf.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(jenisNilaiHuruf, new String[] { "nama", "kode" }, "keterangan",
						JenisNilaiHuruf.class, "=Tanpa Jenis Nilai Huruf=",
						Restrictions.and(Restrictions.eq("sekolah", s), Restrictions.eq("aktif", true)));
				Common.selectComboItem(true, jenisNilaiHuruf, grupPenilaian.getJenisNilaiHuruf());

			}

		};

		sekolah.addEventListener("onChange", eventListener);
		Common.createDefaultTimer(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(grupPenilaian.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Formula"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		array = new JSONArray(grupPenilaian.getFormula());
		rowFormula = Common.tampilanScroll1(row);
		selectedGrupKategoriItemPenilaianSiswa = new HashMap<Long, DetailGrupPenilaian>();
		eventListeners = new ArrayList<EventListener>();
		reloadFormula(rowFormula, null, array, grupPenilaian, selectedGrupKategoriItemPenilaianSiswa, eventListeners);

		rowJp = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowJp, "2");
		rowJp.setParent(rows);

		EventListener ubahJenisPenialain = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Yayasan y = (Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue());
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues.simpleList(
						HibernateUtil.currentSession().createCriteria(GrupKategoriItemPenilaianSiswa.class)

								.add(s == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("sekolah"),
												Restrictions.eq("sekolah", s)))

								.add(y == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("yayasan"),
												Restrictions.eq("yayasan", y)))

								.addOrder(Order.asc("kode"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						GrupKategoriItemPenilaianSiswa.class);

				if (grupPenilaian.getId() != null) {
					HibernateUtil.currentSession().refresh(grupPenilaian);
				}

				if (grupPenilaian.getId() != null) {
					Session session = HibernateUtil.currentSession();
					List<DetailGrupPenilaian> detailGrupPenilaians = ConstantValues
							.simpleList(
									session.createCriteria(DetailGrupPenilaian.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.createAlias("grupKategoriItemPenilaianSiswa",
													"grupKategoriItemPenilaianSiswa")
											.add(Restrictions.or(
													Restrictions.isNull("grupKategoriItemPenilaianSiswa.aktif"),
													Restrictions.eq("grupKategoriItemPenilaianSiswa.aktif", true)))
											.add(Restrictions.eq("grupPenilaian", grupPenilaian)),
									DetailGrupPenilaian.class);

					selectedGrupKategoriItemPenilaianSiswa.clear();
					for (DetailGrupPenilaian detailGrupPenilaian : detailGrupPenilaians) {
						if (!selectedGrupKategoriItemPenilaianSiswa
								.containsKey(detailGrupPenilaian.getGrupKategoriItemPenilaianSiswa().getId())) {
							selectedGrupKategoriItemPenilaianSiswa.put(
									detailGrupPenilaian.getGrupKategoriItemPenilaianSiswa().getId(),
									detailGrupPenilaian);
						}
					}

				} else {
					selectedGrupKategoriItemPenilaianSiswa.clear();
				}

				Common.clear(rowJp);
				MyGrid vboxSkala = new MyGrid();
				vboxSkala.setParent(rowJp);

				Columns columns = new Columns();
				columns.setParent(vboxSkala);

				MyColumnConfig column = new MyColumnConfig("Pilih Grup Kategori");
				column.setParent(columns);

				Rows rowsSkala = new Rows();
				rowsSkala.setParent(vboxSkala);

				for (final GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {

					MyFormRow rowSkala = new MyFormRow();
					rowSkala.setStyle("border:0px;background: transparent;");
					rowSkala.setParent(rowsSkala);

					DetailGrupPenilaian detailGrupPenilaianTemp = selectedGrupKategoriItemPenilaianSiswa
							.get(grupKategoriItemPenilaianSiswa.getId());
					if (detailGrupPenilaianTemp == null) {
						detailGrupPenilaianTemp = new DetailGrupPenilaian();
					}
					detailGrupPenilaianTemp.setGrupKategoriItemPenilaianSiswa(grupKategoriItemPenilaianSiswa);
					final DetailGrupPenilaian detailGrupPenilaian = detailGrupPenilaianTemp;

					final Checkbox checkbox = new Checkbox(grupKategoriItemPenilaianSiswa.getKode() + " - "
							+ grupKategoriItemPenilaianSiswa.getNama());
					checkbox.setAttribute("detailGrupPenilaian", detailGrupPenilaian);
					checkbox.setParent(rowSkala);
					checkbox.setChecked(
							selectedGrupKategoriItemPenilaianSiswa.containsKey(grupKategoriItemPenilaianSiswa.getId()));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (checkbox.isChecked()) {
								selectedGrupKategoriItemPenilaianSiswa.put(grupKategoriItemPenilaianSiswa.getId(),
										detailGrupPenilaian);
							} else {
								selectedGrupKategoriItemPenilaianSiswa.remove(grupKategoriItemPenilaianSiswa.getId());
							}
							for (EventListener eventListener : eventListeners) {
								eventListener.onEvent(arg0);
							}
						}
					});

				}
			}
		};

		yayasan.addEventListener("onChange", ubahJenisPenialain);
		sekolah.addEventListener("onChange", ubahJenisPenialain);

		Common.createDefaultTimer(ubahJenisPenialain);

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

	public static void reloadDataFormula(final Row rowU, final Matapelajaran matapelajaran, final JSONArray array,
			final GrupPenilaian grupPenilaian,
			final HashMap<Long, DetailGrupPenilaian> selectedGrupKategoriItemPenilaianSiswa,
			final List<EventListener> eventListeners) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Tanggal Efektif");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("Formula Total");
		column.setParent(columns);

		column = new MyColumnConfig("Formula Min");
		column.setParent(columns);

		column = new MyColumnConfig("Formula Max");
		column.setParent(columns);

		column = new MyColumnConfig("Nilai");
		column.setWidth("0%");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("10%");
		column.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

			if (!jsonObject.isNull("tgl")) {

				Date tgl = new Date();

				if (!jsonObject.isNull("tgl")) {
					tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());
				}

				String target = "";
				if (!jsonObject.isNull("target")) {
					target = jsonObject.get("target") + "";
				}

				String targetMin = "";
				if (!jsonObject.isNull("target_min")) {
					targetMin = jsonObject.get("target_min") + "";
				}

				String targetMax = "";
				if (!jsonObject.isNull("target_max")) {
					targetMax = jsonObject.get("target_max") + "";
				}

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				final MyTextbox targetText = new MyTextbox(target);

				final MyTextbox targetTextMin = new MyTextbox(targetMin);
				final MyTextbox targetTextMax = new MyTextbox(targetMax);

				final MyDatebox datebox = new MyDatebox(tgl);
				final Label nilai = new Label(Common.numberFormat.get().format(GrupPenilaianUtil.ambilPoint(jsonObject,
						matapelajaran, grupPenilaian, selectedGrupKategoriItemPenilaianSiswa)));
				datebox.setWidth("90%");
				nilai.setWidth("90%");
				row.appendChild(datebox);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						jsonObject.put("tgl",
								datebox.getValue() == null ? "" : Common.dateFormat1.get().format(datebox.getValue()));

						String target = targetText.getValue() == null ? "" : targetText.getValue();
						jsonObject.put("target", target);

						String target_min = targetTextMin.getValue() == null ? "" : targetTextMin.getValue();
						jsonObject.put("target_min", target_min);

						String target_max = targetTextMax.getValue() == null ? "" : targetTextMax.getValue();
						jsonObject.put("target_max", target_max);

						nilai.setValue(Common.numberFormat.get().format(GrupPenilaianUtil.ambilPoint(jsonObject,
								matapelajaran, grupPenilaian, selectedGrupKategoriItemPenilaianSiswa)));
					}
				};

				if (eventListeners != null) {
					eventListeners.add(eventListener);
				}

				targetText.setWidth("90%");
				targetText.setRows(3);
				row.appendChild(targetText);

				targetTextMin.setWidth("90%");
				targetTextMin.setRows(3);
				row.appendChild(targetTextMin);

				targetTextMax.setWidth("90%");
				targetTextMax.setRows(3);
				row.appendChild(targetTextMax);

				row.appendChild(nilai);

				datebox.addEventListener("onChange", eventListener);
				targetText.addEventListener("onChange", eventListener);
				targetTextMin.addEventListener("onChange", eventListener);
				targetTextMax.addEventListener("onChange", eventListener);

				Common.createDefaultTimer(eventListener);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												array.put(index, new JSONObject());

												reloadDataFormula(rowU, matapelajaran, array, grupPenilaian,
														selectedGrupKategoriItemPenilaianSiswa, eventListeners);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																+ e.getMessage());
											}

										}

									}
								});

					}
				});
				button.setParent(row);
			}
		}
	}

	public static void reloadFormula(final Row rowFormula, final Matapelajaran matapelajaran, final JSONArray array,
			final GrupPenilaian grupPenilaian,
			final HashMap<Long, DetailGrupPenilaian> selectedGrupKategoriItemPenilaianSiswa,
			final List<EventListener> eventListeners) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Formula", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("tgl", Common.dateFormat1.get().format(new Date()));
				jsonObject.put("target", "");
				array.put(jsonObject);

				reloadDataFormula(rowU, matapelajaran, array, grupPenilaian, selectedGrupKategoriItemPenilaianSiswa,
						eventListeners);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, matapelajaran, array, grupPenilaian, selectedGrupKategoriItemPenilaianSiswa,
				eventListeners);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Grup Penilaian harus diisi", "Peringatan", MyMessageboxConfig.OK,
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
		if (grupPenilaian.getId() != null) {
			grupPenilaian = (GrupPenilaian) session.load(GrupPenilaian.class, grupPenilaian.getId());
		}
		grupPenilaian.setJenisNilaiHuruf((JenisNilaiHuruf) (jenisNilaiHuruf.getSelectedItem() == null ? null
				: jenisNilaiHuruf.getSelectedItem().getValue()));
		grupPenilaian.setNama(nama.getValue());
		grupPenilaian.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		grupPenilaian.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		grupPenilaian.setKeterangan(keterangan.getValue());
		grupPenilaian.setFormula(array.toString());

		grupPenilaian.setKhususSemester((Integer) (khususSemester.getSelectedItem() == null ? null
				: khususSemester.getSelectedItem().getValue()));
		grupPenilaian.setKhususTingkat((Integer) (khususTingkat.getSelectedItem() == null ? null
				: khususTingkat.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, grupPenilaian);
		session.flush();

		List<DetailGrupPenilaian> d = ConstantValues.simpleList(
				session.createCriteria(DetailGrupPenilaian.class).add(Restrictions.eq("grupPenilaian", grupPenilaian)),
				DetailGrupPenilaian.class);
		for (DetailGrupPenilaian detailGrupPenilaian : d) {
			detailGrupPenilaian.setAktif(false);
			Common.refreshSaveOrUpdate(session, detailGrupPenilaian);
			session.flush();
		}

		if (selectedGrupKategoriItemPenilaianSiswa != null) {
			for (DetailGrupPenilaian detailGrupPenilaian : selectedGrupKategoriItemPenilaianSiswa.values()) {
				detailGrupPenilaian.setAktif(true);
				detailGrupPenilaian.setGrupPenilaian(grupPenilaian);
				Common.refreshSaveOrUpdate(session, detailGrupPenilaian);
				session.flush();
			}
		}
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GrupPenilaian.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<GrupPenilaian> grupPenilaian = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(grupPenilaian);
		grid.setRowRenderer(new GrupPenilaianRenderer());
		grid.setModelCheckMobile(strset);

	}

}
