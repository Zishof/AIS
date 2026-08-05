package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
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

import ais.action.master.helper.KonfigurasiKalenderAkademikHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.KonfigurasiKalenderAkademikProcessor;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.KalenderAkademik;
import ais.database.model.KonfigurasiKalenderAkademik;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class KalenderAkademikAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4733551737383264330L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchJenjang;
	private Combobox searchSemester;
	private Combobox searchProgram;
	private Combobox searchTahunAjaran;
	private Combobox searchGanjilGenap;
	private MyDatebox searchTanggalMulai;
	private MyDatebox searchTanggalSelesai;

	private Checkbox searchaktif;

	private Textbox namaKegiatanAkademik;
	private Textbox deskripsiKegiatanAkademik;
	private Textbox searchkonfigurasi;
	private Combobox jenisKegiatan;
	private MyDatebox tanggalMulai;
	private MyDatebox tanggalSelesai;
	private Textbox ditetapkanOleh;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox jenjang;
	private Combobox semester;
	private Combobox program;
	private boolean edit;
	private boolean delete;
	private Combobox tahunAjaran;
	private Combobox ganjilGenap;
	private Combobox masukDiSmt;

	private KalenderAkademik kalenderAkademik;
	private KonfigurasiKalenderAkademikHelper konfigurasiInlineHelper;
	private Div konfigurasiInlinePanel;

	private Hbox hbFakultasLabel;
	private Hbox hbFakultas;

	private Hbox hbYayasanLabel;
	private Hbox hbYayasan;

	private MyToolbarbuttonConfig add;
	private Sekolah sekolah1;
	private boolean pt;
	private boolean ya;
	private Combobox yayasan;
	private Combobox sekolah;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.insertComboDanSemua(searchJenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(jenisKegiatan = new Combobox(), "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		if (searchTahunAjaran != null) { searchTahunAjaran.setSelectedItem(null); }

		jurusan = new Combobox();
		fakultas = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		sekolah1 = SekolahUtil.getSekolah();

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null) {
			pt = true;
			ya = false;
		} else if (tbmuser.getSiswa() != null || tbmuser.ambilGuru() != null) {
			pt = false;
			ya = true;
		}

		if (hbFakultasLabel != null) {
			hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1);
		}
		if (hbFakultas != null) {
			hbFakultas.setVisible(pt);
		}
		if (hbYayasanLabel != null) {
			hbYayasanLabel.setVisible(ya);
		}

		if (hbYayasan != null) {
			hbYayasan.setVisible(ya);
		}

		if (sekolah1 != null && sekolah1.getId() != null) {
			if (hbFakultasLabel != null) {
				hbFakultasLabel.setVisible(false);
			}
			if (hbFakultas != null) {
				hbFakultas.setVisible(false);
			}
			if (hbYayasanLabel != null) {
				hbYayasanLabel.setVisible(true);
			}

			if (hbYayasan != null) {
				hbYayasan.setVisible(true);
			}

		}

		int maxSemesterPilihan = 25;
		try {
			maxSemesterPilihan = Integer
					.parseInt(Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		for (int i = 1; i < maxSemesterPilihan; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			searchSemester.appendChild(comboitem);
		}

		semester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(0); }
		semester.appendChild(comboitem);

		for (int i = 1; i < maxSemesterPilihan; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}

		program = Common.initPrograms(program);

		Common.initPrograms(searchProgram);

		ganjilGenap = new Combobox();
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		ganjilGenap.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.SP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.SP); }
		ganjilGenap.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		ganjilGenap.appendChild(comboitem);

		if (ganjilGenap != null) { ganjilGenap.setSelectedItem(comboitem); }
		if (ganjilGenap != null) { ganjilGenap.setReadonly(true); }

		masukDiSmt = new Combobox();
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		masukDiSmt.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		masukDiSmt.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		masukDiSmt.appendChild(comboitem);

		if (masukDiSmt != null) { masukDiSmt.setSelectedItem(comboitem); }
		if (masukDiSmt != null) { masukDiSmt.setReadonly(true); }

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchGanjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchGanjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.SP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.SP); }
		searchGanjilGenap.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchGanjilGenap.appendChild(comboitem);

		if (searchGanjilGenap != null) { searchGanjilGenap.setSelectedItem(comboitem); }
		if (searchGanjilGenap != null) { searchGanjilGenap.setReadonly(true); }

		Common.insertCombo(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		MyComboitemConfig comboitem2 = new MyComboitemConfig();
		if (comboitem2 != null) { comboitem2.setValue(null); }
		if (comboitem2 != null) { comboitem2.setLabel("Semua"); }
		jenjang.appendChild(comboitem2);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "namaKegiatanAkademik", "deskripsiKegiatanAkademik", "jenisKegiatan",
				"tanggalMulai", "tanggalSelesai", "ditetapkanOleh", "fakultas", "jurusan", "yayasan", "sekolah",
				"semester", "jenjang", "program", "hari", "tahunAjaran", "ganjilGenap", "masukDiSmt", "status",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KalenderAkademik.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KalenderAkademik.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class KalenderAkademikRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KalenderAkademik kalenderAkademik = (KalenderAkademik) arg1;

			MyDetail detail = new MyDetail();
			arg0.setAttribute("detail", detail);

			final EventListener myEventListener = new EventListener() {

				private void reloadLagi() {
					((MyDetail) arg0.getAttribute("detail")).setOpen(true);
					Common.createDefaultTimer(this);
				}

				@Override
				public void onEvent(Event event) throws Exception {
					// Common.clear(detail);
					if (((MyDetail) arg0.getAttribute("detail")).getChildren().isEmpty()
							&& ((MyDetail) arg0.getAttribute("detail")).isOpen())
						new KonfigurasiKalenderAkademikHelper(new EventListener() {

							@Override
							public void onEvent(Event a) throws Exception {
								Common.clear(arg0);
								render(arg0, kalenderAkademik);
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										reloadLagi();
									}
								});

							}
						}).display(kalenderAkademik, ((MyDetail) arg0.getAttribute("detail")));
				}

			};

			detail.setParent(arg0);
			detail.addEventListener("onOpen", myEventListener);

			Vbox c;
			(c = RevisiHelper.createNewRevisi(KalenderAkademik.class, kalenderAkademik,
					kalenderAkademik.getNamaKegiatanAkademik())).setParent(arg0);
			Session session = HibernateUtil.currentSession();
			List<KonfigurasiKalenderAkademik> konfigurasiKalenderAkademiks = ConstantValues
					.simpleList(
							session.createCriteria(KonfigurasiKalenderAkademik.class).addOrder(Order.asc("id"))
									.add(Restrictions.eq("kalenderAkademik", kalenderAkademik)),
							KonfigurasiKalenderAkademik.class);
			for (KonfigurasiKalenderAkademik konfigurasiKalenderAkademik : konfigurasiKalenderAkademiks) {
				new MyLabelAgakKecil(konfigurasiKalenderAkademik.getKonfigurasi().getNama() + ""
						+ (konfigurasiKalenderAkademik.getKonfigurasi().getInfo1().isEmpty() ? ""
								: "-" + konfigurasiKalenderAkademik.getKonfigurasi().getInfo1())
						+ (konfigurasiKalenderAkademik.getKonfigurasi().getInfo2().isEmpty() ? ""
								: "-" + konfigurasiKalenderAkademik.getKonfigurasi().getInfo2())
						+ (konfigurasiKalenderAkademik.getKonfigurasi().getInfo3().isEmpty() ? ""
								: "-" + konfigurasiKalenderAkademik.getKonfigurasi().getInfo3()))
						.setParent(c);
			}

			new Label(kalenderAkademik.getDeskripsiKegiatanAkademik()).setParent(arg0);

			new Label(kalenderAkademik.getTahunAjaran() == null || kalenderAkademik.getTahunAjaran().trim().equals("")
					? "Semua"
					: kalenderAkademik.getTahunAjaran()).setParent(arg0);
			arg0.setStyle(kalenderAkademik.getWarna());
			new Label(kalenderAkademik.getStatus()).setParent(arg0);
			new Label(kalenderAkademik.getTanggalMulai() == null ? ""
					: Common.dateFormat4.get().format(kalenderAkademik.getTanggalMulai())).setParent(arg0);
			new Label(kalenderAkademik.getTanggalSelesai() == null ? ""
					: Common.dateFormat4.get().format(kalenderAkademik.getTanggalSelesai())).setParent(arg0);
			new Label(kalenderAkademik.getDitetapkanOleh()).setParent(arg0);
			new Label(kalenderAkademik.getFakultas() == null
					? (kalenderAkademik.getYayasan() == null ? "Semua" : kalenderAkademik.getYayasan().getNama())
					: kalenderAkademik.getFakultas().getNama()).setParent(arg0);
			new Label(kalenderAkademik.getJurusan() == null
					? (kalenderAkademik.getSekolah() == null ? "Semua" : kalenderAkademik.getSekolah().getNama())
					: kalenderAkademik.getJurusan().getNama()).setParent(arg0);
			new Label(kalenderAkademik.getSemester() == null || kalenderAkademik.getSemester().equals(0) ? "Semua"
					: kalenderAkademik.getSemester() + "").setParent(arg0);
			new Label(kalenderAkademik.getJenjang() == null ? "Semua" : kalenderAkademik.getJenjang().getNama())
					.setParent(arg0);
			new Label(kalenderAkademik.getProgram() == null || kalenderAkademik.getProgram().trim().equals("") ? "Semua"
					: kalenderAkademik.getProgram()).setParent(arg0);
			new Label(kalenderAkademik.getGanjilGenap() == null ? "Semua" : kalenderAkademik.getGanjilGenap())
					.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kalenderAkademik.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kalenderAkademik.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kalenderAkademik);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kalenderAkademik);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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

											Common.refreshDelete(kalenderAkademik);

											onSearchDefault(event);
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
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KalenderAkademik());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final KalenderAkademik kalenderAkademik) {
		this.kalenderAkademik = kalenderAkademik;
		addWindow.setTitle(kalenderAkademik.getId() == null ? "Tambah Kalender Akademik" : "Ubah Kalender Akademik");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		int[] tabAktif = {0};
		final ais.ui.util.MyButtonTabbox btnTabKalender = ais.ui.util.MyButtonTabbox.buat(center, "100%", tabAktif);

		btnTabKalender.tambahTabLazy(0, "Informasi", null, new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(panel);
				grid.setHeight("100%");

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan"));
				row.appendChild(namaKegiatanAkademik = new Textbox(kalenderAkademik.getNamaKegiatanAkademik()));
				namaKegiatanAkademik.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi Kegiatan"));
				row.appendChild(deskripsiKegiatanAkademik = new Textbox(kalenderAkademik.getNamaKegiatanAkademik()));
				deskripsiKegiatanAkademik.setWidth("90%");
				deskripsiKegiatanAkademik.setRows(4);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
				tahunAjaran = Common.generateTahunAjaran(tahunAjaran);
				row.appendChild(tahunAjaran);
				tahunAjaran.setWidth("90%");
				Common.sisipkanSemuaDiCombo(tahunAjaran, null);
				Common.selectComboItem(tahunAjaran, kalenderAkademik.getTahunAjaran());

				row = new MyFormRow();
				row.setVisible(false);
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kegiatan"));
				Common.selectComboItem(jenisKegiatan, kalenderAkademik.getJenisKegiatan());
				row.appendChild(jenisKegiatan);
				jenisKegiatan.setWidth("90%");
				Common.sisipkanSemuaDiCombo(jenisKegiatan, null);
				jenisKegiatan.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
				row.appendChild(tanggalMulai = new MyDatebox(
						kalenderAkademik.getTanggalMulai() == null ? ais.ui.util.WaktuUtil.getDate()
								: kalenderAkademik.getTanggalMulai()));
				tanggalMulai.setFormat(Common.dateFormat1.get().toPattern());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Selesai"));
				row.appendChild(tanggalSelesai = new MyDatebox(
						kalenderAkademik.getTanggalSelesai() == null ? ais.ui.util.WaktuUtil.getDate()
								: kalenderAkademik.getTanggalSelesai()));
				tanggalSelesai.setFormat(Common.dateFormat1.get().toPattern());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Di-tetapkan Oleh"));
				row.appendChild(ditetapkanOleh = new Textbox(
						kalenderAkademik.getDitetapkanOleh() == null ? Common.getCurrentUser().getUserId()
								: kalenderAkademik.getDitetapkanOleh()));
				ditetapkanOleh.setWidth("90%");

				Tbmuser tbmuser = Common.getCurrentUser();
				row = new MyFormRow();
				row.setVisible(pt);
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
				Common.selectComboItem(fakultas,
						kalenderAkademik.getFakultas() == null ? tbmuser.ambilFakultas() : kalenderAkademik.getFakultas());
				row.appendChild(fakultas);
				fakultas.setWidth("90%");
				Common.sisipkanSemuaDiCombo(fakultas, null);
				fakultas.setReadonly(true);

				if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
					Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
							CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
				}

				row = new MyFormRow();
				row.setVisible(pt);
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
				Common.pilihJurusan(jurusan,
						kalenderAkademik.getJurusan() == null ? tbmuser.ambilJurusan() : kalenderAkademik.getJurusan());
				row.appendChild(jurusan);
				jurusan.setWidth("90%");
				Common.sisipkanSemuaDiCombo(jurusan, null);
				jurusan.setReadonly(true);

				row = new MyFormRow();
				row.setVisible(pt);
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
				Common.selectComboItem(jenjang, kalenderAkademik.getJenjang());
				row.appendChild(jenjang);
				jenjang.setWidth("90%");
				Common.sisipkanSemuaDiCombo(jenjang, null);
				jenjang.setReadonly(true);

				yayasan = new Combobox();
				sekolah = new Combobox();
				Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

				row = new MyFormRow();
				row.setVisible(ya);
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

				Common.selectComboItem(yayasan,
						kalenderAkademik == null || kalenderAkademik.getYayasan() == null ? tbmuser.ambilYayasan()
								: kalenderAkademik.getYayasan());
				row.appendChild(yayasan);
				yayasan.setWidth("90%");

				row = new MyFormRow();
				row.setVisible(ya);
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

				Common.pilihSekolah(sekolah,
						kalenderAkademik == null || kalenderAkademik.getSekolah() == null ? tbmuser.ambilSekolah()
								: kalenderAkademik.getSekolah());
				row.appendChild(sekolah);
				sekolah.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
				Common.selectComboItem(semester, kalenderAkademik.getSemester());
				row.appendChild(semester);
				semester.setWidth("90%");
				semester.setReadonly(true);
				if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
					semester.setSelectedIndex(0);
				}

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
				Common.selectComboItem(program, kalenderAkademik.getProgram());
				row.appendChild(program);
				program.setWidth("90%");
				Common.sisipkanSemuaDiCombo(program, null);
				program.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
				Common.selectComboItem(ganjilGenap, kalenderAkademik.getGanjilGenap());
				row.appendChild(ganjilGenap);
				ganjilGenap.setWidth("90%");
				ganjilGenap.setReadonly(true);
				// Common.sisipkanSemuaDiCombo(ganjilGenap, null);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Khusus mahasiswa yang masuk di semester"));
				Common.selectComboItem(masukDiSmt, kalenderAkademik.getMasukDiSmt());
				row.appendChild(masukDiSmt);
				masukDiSmt.setWidth("90%");
				masukDiSmt.setReadonly(true);

				if (sekolah1 != null && sekolah1.getId() != null) {
					fakultas.getParent().setVisible(false);
					jurusan.getParent().setVisible(false);
					program.getParent().setVisible(false);

					sekolah.getParent().setVisible(true);
					yayasan.getParent().setVisible(true);
				}
			}
		});

		btnTabKalender.tambahTabLazy(1, "Konfigurasi", null, new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				if (kalenderAkademik.getId() == null) {
					Div msg = new Div();
					msg.setStyle("padding:16px;color:#888;");
					new org.zkoss.zul.Label(
						"Simpan data kegiatan akademik terlebih dahulu untuk mengatur konfigurasi."
					).setParent(msg);
					msg.setParent(panel);
				} else {
					konfigurasiInlinePanel = panel;
					konfigurasiInlineHelper = new KonfigurasiKalenderAkademikHelper(new EventListener() {
						@Override
						public void onEvent(Event e) throws Exception {}
					});
					konfigurasiInlineHelper.displayPilihanInline(kalenderAkademik, panel);
				}
			}
		});

		btnTabKalender.pilih(0);

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
					if (konfigurasiInlineHelper != null && !konfigurasiInlineHelper.simpanInline(event)) {
						return;
					}
					if (konfigurasiInlineHelper != null && konfigurasiInlinePanel != null) {
						konfigurasiInlineHelper.refreshInline(kalenderAkademik, konfigurasiInlinePanel);
					}
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (namaKegiatanAkademik.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data kegiatan akademik",
					"Kolom Nama kegiatan akademik belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama kegiatan akademik.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kalenderAkademik.getId() != null) {
			kalenderAkademik = (KalenderAkademik) session.load(KalenderAkademik.class, kalenderAkademik.getId());
		}
		kalenderAkademik.setDeskripsiKegiatanAkademik(deskripsiKegiatanAkademik.getValue());
		kalenderAkademik.setNamaKegiatanAkademik(namaKegiatanAkademik.getValue());
		kalenderAkademik.setTahunAjaran(
				tahunAjaran.getSelectedItem() == null ? null : (String) tahunAjaran.getSelectedItem().getValue());
		kalenderAkademik.setGanjilGenap(
				ganjilGenap.getSelectedItem() == null ? null : (String) ganjilGenap.getSelectedItem().getValue());

		kalenderAkademik.setMasukDiSmt(
				masukDiSmt.getSelectedItem() == null || masukDiSmt.getSelectedItem().getValue() == null ? null
						: (String) masukDiSmt.getSelectedItem().getValue());

		kalenderAkademik.setJenisKegiatan((JenisKegiatan) (jenisKegiatan.getSelectedItem() == null ? null
				: jenisKegiatan.getSelectedItem().getValue()));
		kalenderAkademik.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		kalenderAkademik.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		kalenderAkademik.setJenjang(
				(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));
		kalenderAkademik.setDitetapkanOleh(ditetapkanOleh.getValue());
		kalenderAkademik.setTanggalMulai(tanggalMulai.getValue());
		kalenderAkademik.setTanggalSelesai(tanggalSelesai.getValue());
		kalenderAkademik.setSemester(
				semester.getSelectedItem() == null ? null : (Integer) semester.getSelectedItem().getValue());
		kalenderAkademik.setProgram(program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
				|| program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue().toString());

		kalenderAkademik.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		kalenderAkademik.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, kalenderAkademik);

		KonfigurasiKalenderAkademikProcessor.doProcess();

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> idsKonfigurasi = new ArrayList<Long>();
		if (!searchkonfigurasi.getValue().trim().isEmpty()) {
			idsKonfigurasi = session.createCriteria(KonfigurasiKalenderAkademik.class)
					.setProjection(Projections.groupProperty("kalenderAkademik.id"))
					.createAlias("konfigurasi", "konfigurasi").add(Restrictions.ilike("konfigurasi.nama",
							searchkonfigurasi.getValue().trim(), MatchMode.ANYWHERE))
					.list();
		}

		Criteria criteria = session.createCriteria(KalenderAkademik.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(!searchkonfigurasi.getValue().trim().isEmpty() && idsKonfigurasi.isEmpty()
						? Restrictions.sqlRestriction("false")
						: (idsKonfigurasi.isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.in("id", idsKonfigurasi)));
		if (order)
			criteria.addOrder(Order.desc("tanggalMulai"));
		if (order)
			criteria.addOrder(Order.desc("tanggalSelesai"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(
						Restrictions.ilike("namaKegiatanAkademik", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("deskripsiKegiatanAkademik", searchnama.getValue().trim(),
								MatchMode.ANYWHERE)))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchGanjilGenap.getSelectedItem() == null
						|| searchGanjilGenap.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("ganjilGenap", searchGanjilGenap.getSelectedItem().getValue()))

				.add(searchSemester.getSelectedItem() == null || searchSemester.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("semester", searchSemester.getSelectedItem().getValue()))

				.add((searchTanggalMulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchTanggalMulai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tanggalMulai", searchTanggalMulai.getValue())))

				.add((searchTanggalSelesai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchTanggalSelesai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tanggalSelesai", searchTanggalSelesai.getValue())))

				.add(searchProgram.getSelectedItem() == null || searchProgram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program",
								searchProgram.getSelectedItem() == null ? "Reguler"
										: searchProgram.getSelectedItem().getValue()))

				.add(searchJenjang.getSelectedItem() == null || searchJenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchJenjang.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

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

		List<KalenderAkademik> kalenderAkademik = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(kalenderAkademik);
		grid.setRowRenderer(new KalenderAkademikRenderer());
		grid.setModelCheckMobile(strset);

	}

}
