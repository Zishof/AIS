package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.KrsDanSkripsiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.FormatNilaiSkripsiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilaiSkripsi;
import ais.database.model.JenisKegiatanMahasiswa;
import ais.database.model.JenisNilaiHurufMatakuliah;
import ais.database.model.Jurusan;
import ais.database.model.KomponenPenilaianSkripsi;
import ais.database.model.Matakuliah;
import ais.database.model.SkripsiPunyaKomponenPenilaianSkripsi;
import ais.database.model.Tbmuser;
import ais.database.model.library.TipeItem;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class FormatNilaiSkripsiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Checkbox searchaktif;

	private MyDoublebox ketuaSidang;
	private MyDoublebox pembimbing1;

	private FormatNilaiSkripsi formatNilaiSkripsi;
	private MyDoublebox penguji1;
	private MyDoublebox penguji2;
	private MyDoublebox penguji3;
	private MyDoublebox penguji4;

	private MyTextbox dosen1;
	private MyTextbox dosen2;
	private MyTextbox dosen21;
	private MyTextbox dosen3;
	private MyTextbox dosen4;
	private MyTextbox dosen5;
	private MyTextbox dosen6;

	private MyTextbox uploadLampiran1;
	private MyTextbox uploadLampiran2;
	private MyTextbox uploadLampiran3;
	private MyTextbox uploadLampiran4;
	private MyTextbox uploadLampiran5;
	private MyTextbox uploadLampiran6;
	private MyTextbox uploadLampiran7;
	private MyTextbox uploadLampiran8;
	private MyTextbox uploadLampiran9;
	private MyTextbox uploadLampiran10;

	private MyTextbox uploadLampiran11;
	private MyTextbox uploadLampiran12;
	private MyTextbox uploadLampiran13;
	private MyTextbox uploadLampiran14;
	private MyTextbox uploadLampiran15;
	private MyTextbox uploadLampiran16;
	private MyTextbox uploadLampiran17;
	private MyTextbox uploadLampiran18;
	private MyTextbox uploadLampiran19;
	private MyTextbox uploadLampiran20;

	private MyCheckboxConfig uploadLampiran1Wajib;
	private MyCheckboxConfig uploadLampiran2Wajib;
	private MyCheckboxConfig uploadLampiran3Wajib;
	private MyCheckboxConfig uploadLampiran4Wajib;
	private MyCheckboxConfig uploadLampiran5Wajib;
	private MyCheckboxConfig uploadLampiran6Wajib;
	private MyCheckboxConfig uploadLampiran7Wajib;
	private MyCheckboxConfig uploadLampiran8Wajib;
	private MyCheckboxConfig uploadLampiran9Wajib;
	private MyCheckboxConfig uploadLampiran10Wajib;

	private MyCheckboxConfig uploadLampiran11Wajib;
	private MyCheckboxConfig uploadLampiran12Wajib;
	private MyCheckboxConfig uploadLampiran13Wajib;
	private MyCheckboxConfig uploadLampiran14Wajib;
	private MyCheckboxConfig uploadLampiran15Wajib;

	private MyCheckboxConfig uploadLampiran16Wajib;
	private MyCheckboxConfig uploadLampiran17Wajib;
	private MyCheckboxConfig uploadLampiran18Wajib;
	private MyCheckboxConfig uploadLampiran19Wajib;
	private MyCheckboxConfig uploadLampiran20Wajib;

	private Combobox tipeItem1;
	private Combobox tipeItem2;
	private Combobox tipeItem3;
	private Combobox tipeItem4;
	private Combobox tipeItem5;
	private Combobox tipeItem6;
	private Combobox tipeItem7;
	private Combobox tipeItem8;
	private Combobox tipeItem9;
	private Combobox tipeItem10;
	private Combobox tipeItem11;
	private Combobox tipeItem12;
	private Combobox tipeItem13;
	private Combobox tipeItem14;
	private Combobox tipeItem15;

	private Combobox tipeItem16;
	private Combobox tipeItem17;
	private Combobox tipeItem18;
	private Combobox tipeItem19;
	private Combobox tipeItem20;

	private MyCheckboxConfig tidakWajibMengambilMkTertentu;
	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;
	private MyTextbox nama;
	private Combobox fakultas;
	private Combobox jurusan;

	private List<KomponenPenilaianSkripsi> selectedKomponenPenilaianSkripsi;
	private MyTextbox kodeMatakuliah;
	private MyDoublebox bobot;
	private MyIntbox minimalSks;
	private MyDoublebox minimalIpk;
	private MyDoublebox minimalAngkaKredit;
	private MyTextbox matkulPrasyaratLulus;
	private MyCheckboxConfig harusLunas;
	private MyDoublebox prosentaseLunas;
	private Textbox kodeItemBiaya;
	private MyCheckboxConfig harusMengembalikanBukuPerpustakaan;
	private MyTextbox kodeMatakuliahDan;
	private Textbox tahunAngkatan;
	private MyCheckboxConfig sekaliBayar;
	private MyDoublebox prosentasiNilaiPembimbing3;
	private MyCheckboxConfig tidakBolehDipilihMahasiswa;
	private MyTextbox dosen7;
	private MyDoublebox penguji5;
	private Center center1;
	private MyCheckboxConfig dosen1Aktif;
	private MyCheckboxConfig dosen2Aktif;
	private MyCheckboxConfig dosen3Aktif;
	private MyCheckboxConfig dosen4Aktif;
	private MyCheckboxConfig dosen5Aktif;
	private MyCheckboxConfig dosen6Aktif;
	private MyCheckboxConfig dosen7Aktif;
	private MyCheckboxConfig dosen21Aktif;
	private MyTextbox kode1;
	private MyTextbox kode2;
	private MyTextbox kode21;
	private MyTextbox kode3;
	private MyTextbox kode4;
	private MyTextbox kode5;
	private MyTextbox kode6;
	private MyTextbox kode7;
	private Combobox jenis;
	private Combobox jenisNilaiHuruf;
	private MyCheckboxConfig mahasiswaBolehMengubahAgendaAtauJadwalBimbingan;

	private Tabpanel jenisTab;

	public void onJenis(Event event) {
		if (jenisTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisTab);
			MyInclude iframe = new MyInclude("/pages/master/jenis_kegiatan_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

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

		Session session = HibernateUtil.currentNativeSession();
		String d = "[{\"id_jns_akt_mhs\":\"1\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Laporan akhir studi\"},{\"id_jns_akt_mhs\":\"10\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Aktivitas kemahasiswaan\"},{\"id_jns_akt_mhs\":\"11\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Program kreativitas mahasiswa\"},{\"id_jns_akt_mhs\":\"12\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Kompetisi\"},{\"id_jns_akt_mhs\":\"13\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Magang\\/Praktik Kerja\"},{\"id_jns_akt_mhs\":\"14\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Asistensi Mengajar di Satuan Pendidikan\"},{\"id_jns_akt_mhs\":\"15\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Penelitian\\/Riset\"},{\"id_jns_akt_mhs\":\"16\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Proyek Kemanusiaan\"},{\"id_jns_akt_mhs\":\"17\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Kegiatan Wirausaha\"},{\"id_jns_akt_mhs\":\"18\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Studi\\/Proyek Independen\"},{\"id_jns_akt_mhs\":\"19\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Membangun Desa\\/Kuliah Kerja Nyata Tematik\"},{\"id_jns_akt_mhs\":\"2\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Tugas akhir\"},{\"id_jns_akt_mhs\":\"3\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Tesis\"},{\"id_jns_akt_mhs\":\"4\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Disertasi\"},{\"id_jns_akt_mhs\":\"5\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Kuliah kerja nyata\"},{\"id_jns_akt_mhs\":\"6\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Kerja praktek\\/PKL\"},{\"id_jns_akt_mhs\":\"7\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Bimbingan akademis\"}]";
		try {

			JSONArray jsonArray = new JSONArray(d);
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					JSONObject jsonObject = jsonArray.getJSONObject(i);

					JenisKegiatanMahasiswa jenisKegiatanMahasiswa = (JenisKegiatanMahasiswa) ConstantValues
							.simpleObject(session.createCriteria(JenisKegiatanMahasiswa.class)
									.add(Restrictions.eq("kode", jsonObject.getString("id_jns_akt_mhs").trim()))
									.setMaxResults(1), JenisKegiatanMahasiswa.class);
					if (jenisKegiatanMahasiswa == null) {
						jenisKegiatanMahasiswa = new JenisKegiatanMahasiswa();
						jenisKegiatanMahasiswa.setKode(jsonObject.getString("id_jns_akt_mhs").trim());
						jenisKegiatanMahasiswa.setNama(jsonObject.get("nm_jns_akt_mhs") + "");
						session.getTransaction().begin();
						session.save(jenisKegiatanMahasiswa);
						session.getTransaction().commit();
					}
				} catch (JSONException e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		} catch (JSONException e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class FormatNilaiSkripsiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final FormatNilaiSkripsi formatNilaiSkripsi = (FormatNilaiSkripsi) arg1;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(formatNilaiSkripsi.getNama()).setParent(vbox);
			if (formatNilaiSkripsi.getJenisKegiatanMahasiswa() != null) {
				new Label(formatNilaiSkripsi.getJenisKegiatanMahasiswa().getNama()).setParent(vbox);
			}

			for (String kode : formatNilaiSkripsi.getKodeMatakuliah().split(",")) {
				if (!kode.trim().isEmpty()) {
					Object[] nama = (Object[]) HibernateUtil.currentSession().createCriteria(Matakuliah.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.projectionList().add(Projections.property("kode"))
									.add(Projections.property("nama")))
							.add(Restrictions.or(Restrictions.ilike("nama", kode.trim(), MatchMode.EXACT),
									Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)))
							.setMaxResults(1).uniqueResult();
					if (nama != null && nama.length > 1) {
						new MyLabelKecil(nama[0] + " - " + nama[1] + " (atau)").setParent(vbox);
					}
				}
			}

			String kodeDanEfektif = KrsDanSkripsiHelper.kodeMatakuliahDanEfektif(
					formatNilaiSkripsi.getKodeMatakuliahDan(), formatNilaiSkripsi.getKodeMatakuliah());
			for (String kode : kodeDanEfektif.split(",")) {
				if (!kode.trim().isEmpty()) {
					Object[] nama = (Object[]) HibernateUtil.currentSession().createCriteria(Matakuliah.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.projectionList().add(Projections.property("kode"))
									.add(Projections.property("nama")))
							.add(Restrictions.or(Restrictions.ilike("nama", kode.trim(), MatchMode.EXACT),
									Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)))
							.setMaxResults(1).uniqueResult();
					if (nama != null && nama.length > 1) {
						new MyLabelKecil(nama[0] + " - " + nama[1] + " (dan)").setParent(vbox);
					}
				}
			}

			new Label(Common.numberFormat.get().format(formatNilaiSkripsi.getMinimalSks()) + " / "
					+ Common.numberFormat.get().format(formatNilaiSkripsi.getMinimalIpk()) + " / "
					+ Common.numberFormat.get().format(formatNilaiSkripsi.getMinimalAngkaKredit()) + " / "
					+ Common.numberFormat.get().format(formatNilaiSkripsi.getBobot())).setParent(arg0);

			new Label(formatNilaiSkripsi.getDosen1() + "/"
					+ (formatNilaiSkripsi.getProsentasiNilaiKetuaSidang() == null ? ""
							: Common.numberFormat.get().format(formatNilaiSkripsi.getProsentasiNilaiKetuaSidang())))
					.setParent(arg0);
			new Label(formatNilaiSkripsi.getDosen2() + "/"
					+ (formatNilaiSkripsi.getProsentasiNilaiPembimbing() == null ? ""
							: Common.numberFormat.get().format(formatNilaiSkripsi.getProsentasiNilaiPembimbing())))
					.setParent(arg0);

			new Label(formatNilaiSkripsi.getDosen21() + "/"
					+ (formatNilaiSkripsi.getProsentasiNilaiPembimbing3() == null ? ""
							: Common.numberFormat.get().format(formatNilaiSkripsi.getProsentasiNilaiPembimbing3())))
					.setParent(arg0);

			new Label(formatNilaiSkripsi.getDosen3() + "/"
					+ (formatNilaiSkripsi.getProsentasiNilaiPenguji1() == null ? ""
							: Common.numberFormat.get().format(formatNilaiSkripsi.getProsentasiNilaiPenguji1())))
					.setParent(arg0);

			new Label(formatNilaiSkripsi.getDosen4() + "/"
					+ (formatNilaiSkripsi.getProsentasiNilaiPenguji2() == null ? ""
							: Common.numberFormat.get().format(formatNilaiSkripsi.getProsentasiNilaiPenguji2())))
					.setParent(arg0);

			new Label(formatNilaiSkripsi.getDosen5() + "/"
					+ (formatNilaiSkripsi.getProsentasiNilaiPenguji3() == null ? ""
							: Common.numberFormat.get().format(formatNilaiSkripsi.getProsentasiNilaiPenguji3())))
					.setParent(arg0);

			new Label(formatNilaiSkripsi.getDosen6() + "/"
					+ (formatNilaiSkripsi.getProsentasiNilaiPenguji4() == null ? ""
							: Common.numberFormat.get().format(formatNilaiSkripsi.getProsentasiNilaiPenguji4())))
					.setParent(arg0);

			new Label(formatNilaiSkripsi.getDosen7() + "/"
					+ (formatNilaiSkripsi.getProsentasiNilaiPenguji5() == null ? ""
							: Common.numberFormat.get().format(formatNilaiSkripsi.getProsentasiNilaiPenguji5())))
					.setParent(arg0);

			new Label(formatNilaiSkripsi.getFakultas() == null ? "Semua" : formatNilaiSkripsi.getFakultas().getNama())
					.setParent(arg0);
			new Label(formatNilaiSkripsi.getJurusan() == null ? "Semua" : formatNilaiSkripsi.getJurusan().getNama())
					.setParent(arg0);

			new Label(formatNilaiSkripsi.getTahunAngkatan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(formatNilaiSkripsi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					formatNilaiSkripsi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(formatNilaiSkripsi);
				}
			});

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(formatNilaiSkripsi);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

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

											Common.refreshDelete(formatNilaiSkripsi);

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
			aksiButtons.add(button);
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new FormatNilaiSkripsi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(FormatNilaiSkripsi formatNilaiSkripsi) {
		this.formatNilaiSkripsi = formatNilaiSkripsi;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		West west = new West();
		west.setTitle("Pendataan Format Nilai");
		west.setParent(borderlayout);
		west.setWidth("40%");

		center1 = new Center();
		center1.setTitle("Komponen Penilaian");
		center1.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center1, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Format"));
		row.appendChild(nama = new MyTextbox(formatNilaiSkripsi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama Matakuliah (Atau)"));
		row.appendChild(kodeMatakuliah = new MyTextbox(formatNilaiSkripsi.getKodeMatakuliah()));
		kodeMatakuliah.setWidth("90%");
		kodeMatakuliah.setRows(2);
		Common.initKeterangan(rows,
				"Jika terdapat banyak kode atau nama matakuliah, pisah menggunakan tanda koma (,). Misal : BSC123,DCFR45,DESW56 maka artinya adalah BSC123 atau DCFR45 atau DESW56");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama Matakuliah (Dan)"));
		row.appendChild(kodeMatakuliahDan = new MyTextbox(formatNilaiSkripsi.getKodeMatakuliahDan()));
		kodeMatakuliahDan.setWidth("90%");
		kodeMatakuliahDan.setRows(2);
		Common.initKeterangan(rows,
				"Jika terdapat banyak kode atau nama matakuliah, pisah menggunakan tanda koma (,). Misal : BSC123,DCFR45,DESW56 maka artinya adalah BSC123 dan DCFR45 dan DESW56");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunAngkatan = new Textbox(formatNilaiSkripsi.getTahunAngkatan()));
		tahunAngkatan.setWidth("90%");

		Common.initKeterangan(rows,
				"(Kosongkan tahun angkatan jika format ini berlaku untuk semua tahun angkatan, jika terdapat banyak tahun angkatan, masukkan tahun angkatan yang dipisahkan koma, contoh 2017,2018,2019");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(tidakWajibMengambilMkTertentu = new MyCheckboxConfig(
				"Mahasiswa Tidak Wajib mengambil Matakuliah Tertentu"));
		tidakWajibMengambilMkTertentu.setChecked(formatNilaiSkripsi.getTidakWajibMengambilMkTertentu());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(tidakBolehDipilihMahasiswa = new MyCheckboxConfig("Tidak Boleh Dipilih Mahasiswa"));
		tidakBolehDipilihMahasiswa.setChecked(formatNilaiSkripsi.getTidakBolehDipilihMahasiswa());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bobot"));
		row.appendChild(bobot = new MyDoublebox(formatNilaiSkripsi.getBobot()));
		bobot.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal SKS"));
		row.appendChild(minimalSks = new MyIntbox(formatNilaiSkripsi.getMinimalSks()));
		minimalSks.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal IPK"));
		row.appendChild(minimalIpk = new MyDoublebox(formatNilaiSkripsi.getMinimalIpk()));
		minimalIpk.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal Angka Kredit"));
		row.appendChild(minimalAngkaKredit = new MyDoublebox(formatNilaiSkripsi.getMinimalAngkaKredit()));
		minimalAngkaKredit.setCols(2);

		// Matakuliah PRASYARAT LULUS untuk boleh mendaftar sidang (kode/nama, dipisah koma).
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah Wajib Lulus (Prasyarat Daftar)"));
		row.appendChild(matkulPrasyaratLulus = new MyTextbox(
				formatNilaiSkripsi.getMatkulPrasyaratLulus() == null ? "" : formatNilaiSkripsi.getMatkulPrasyaratLulus()));
		matkulPrasyaratLulus.setWidth("90%");
		Common.initKeterangan(rows,
				"Isi KODE atau NAMA matakuliah yang WAJIB SUDAH LULUS agar mahasiswa boleh mendaftar sidang, dipisah "
						+ "koma (mis. BSC123,DCFR45). Bila ada yang belum lulus, pendaftaran ditolak. Kosongkan bila tanpa prasyarat matkul.");

		jenisNilaiHuruf = new Combobox();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Nilai Huruf"));
		Common.insertComboDanSemua(jenisNilaiHuruf, new String[] { "nama" }, "keterangan",
				JenisNilaiHurufMatakuliah.class, "Nilai Huruf Default", Restrictions.eq("aktif", true));
		Common.selectComboItem(true, jenisNilaiHuruf, formatNilaiSkripsi.getJenisNilaiHuruf());
		row.appendChild(jenisNilaiHuruf);
		jenisNilaiHuruf.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				formatNilaiSkripsi.getFakultas() == null ? tbmuser.ambilFakultas() : formatNilaiSkripsi.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		Common.initKeterangan(rows, "(Kosongkan " + Common.getBahasaConfig("Fakultas")
				+ " jika format nilai ini berlaku untuk semua " + Common.getBahasaConfig("Fakultas") + ")");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				formatNilaiSkripsi.getJurusan() == null ? tbmuser.ambilJurusan() : formatNilaiSkripsi.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		Common.initKeterangan(rows, "(Kosongkan " + Common.getBahasaConfig("Jurusan")
				+ " jika format nilai ini berlaku untuk semua " + Common.getBahasaConfig("Jurusan") + ")");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		jenis = new Combobox();
//		for (String key : FeederJSONImport.JENIS_KEGIATAN.keySet()) {
//			Comboitem comboitem = new Comboitem(key);
//			comboitem.setValue(FeederJSONImport.JENIS_KEGIATAN.get(key));
//			jenis.appendChild(comboitem);
//		}
//
//		Comboitem comboitem = new Comboitem("Tidak Ditentukan");
//		comboitem.setValue(null);
//		jenis.appendChild(comboitem);

//		Common.selectComboItem(jenis, formatNilaiSkripsi.getJenis());

		Common.insertComboDanSemua(jenis, new String[] { "nama" }, "keterengan", JenisKegiatanMahasiswa.class,
				"Tidak Ditentukan", Restrictions.eq("aktif", true));

		Common.selectComboItem(jenis, formatNilaiSkripsi.getJenisKegiatanMahasiswa());

		row.appendChild(jenis);
		jenis.setWidth("90%");
		jenis.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen1Aktif = new MyCheckboxConfig(),
				dosen1 = new MyTextbox(formatNilaiSkripsi.getDosen1()),
				kode1 = new MyTextbox(formatNilaiSkripsi.getKode1()) }));
		row.appendChild(ketuaSidang = new MyDoublebox(formatNilaiSkripsi.getProsentasiNilaiKetuaSidang()));
		ketuaSidang.setWidth("90%");
		dosen1Aktif.setChecked(formatNilaiSkripsi.getDosen1Aktif());

		kode1.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen2Aktif = new MyCheckboxConfig(),
				dosen2 = new MyTextbox(formatNilaiSkripsi.getDosen2()),
				kode2 = new MyTextbox(formatNilaiSkripsi.getKode2()) }));
		row.appendChild(pembimbing1 = new MyDoublebox(formatNilaiSkripsi.getProsentasiNilaiPembimbing()));
		pembimbing1.setWidth("90%");
		dosen2Aktif.setChecked(formatNilaiSkripsi.getDosen2Aktif());

		kode2.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen21Aktif = new MyCheckboxConfig(),
				dosen21 = new MyTextbox(formatNilaiSkripsi.getDosen21()),
				kode21 = new MyTextbox(formatNilaiSkripsi.getKode21()) }));
		row.appendChild(
				prosentasiNilaiPembimbing3 = new MyDoublebox(formatNilaiSkripsi.getProsentasiNilaiPembimbing3()));
		prosentasiNilaiPembimbing3.setWidth("90%");
		dosen21Aktif.setChecked(formatNilaiSkripsi.getDosen21Aktif());

		kode21.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen3Aktif = new MyCheckboxConfig(),
				dosen3 = new MyTextbox(formatNilaiSkripsi.getDosen3()),
				kode3 = new MyTextbox(formatNilaiSkripsi.getKode3()) }));
		row.appendChild(penguji1 = new MyDoublebox(formatNilaiSkripsi.getProsentasiNilaiPenguji1()));
		penguji1.setWidth("90%");
		dosen3Aktif.setChecked(formatNilaiSkripsi.getDosen3Aktif());

		kode3.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen4Aktif = new MyCheckboxConfig(),
				dosen4 = new MyTextbox(formatNilaiSkripsi.getDosen4()),
				kode4 = new MyTextbox(formatNilaiSkripsi.getKode4()) }));
		row.appendChild(penguji2 = new MyDoublebox(formatNilaiSkripsi.getProsentasiNilaiPenguji2()));
		penguji2.setWidth("90%");
		dosen4Aktif.setChecked(formatNilaiSkripsi.getDosen4Aktif());

		kode4.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen5Aktif = new MyCheckboxConfig(),
				dosen5 = new MyTextbox(formatNilaiSkripsi.getDosen5()),
				kode5 = new MyTextbox(formatNilaiSkripsi.getKode5()) }));
		row.appendChild(penguji3 = new MyDoublebox(formatNilaiSkripsi.getProsentasiNilaiPenguji3()));
		penguji3.setWidth("90%");
		dosen5Aktif.setChecked(formatNilaiSkripsi.getDosen5Aktif());

		kode5.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen6Aktif = new MyCheckboxConfig(),
				dosen6 = new MyTextbox(formatNilaiSkripsi.getDosen6()),
				kode6 = new MyTextbox(formatNilaiSkripsi.getKode6()) }));
		row.appendChild(penguji4 = new MyDoublebox(formatNilaiSkripsi.getProsentasiNilaiPenguji4()));
		penguji4.setWidth("90%");
		dosen6Aktif.setChecked(formatNilaiSkripsi.getDosen6Aktif());

		kode6.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen7Aktif = new MyCheckboxConfig(),
				dosen7 = new MyTextbox(formatNilaiSkripsi.getDosen7()),
				kode7 = new MyTextbox(formatNilaiSkripsi.getKode7()) }));
		row.appendChild(penguji5 = new MyDoublebox(formatNilaiSkripsi.getProsentasiNilaiPenguji5()));
		penguji5.setWidth("90%");
		dosen7Aktif.setChecked(formatNilaiSkripsi.getDosen7Aktif());

		kode7.setCols(3);

		EventListener s = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dosen1.setDisabled(!dosen1Aktif.isChecked());

				ketuaSidang.setDisabled(!dosen1Aktif.isChecked());
				dosen2.setDisabled(!dosen2Aktif.isChecked());
				pembimbing1.setDisabled(!dosen2Aktif.isChecked());

				dosen21.setDisabled(!dosen21Aktif.isChecked());
				prosentasiNilaiPembimbing3.setDisabled(!dosen21Aktif.isChecked());

				dosen3.setDisabled(!dosen3Aktif.isChecked());
				penguji1.setDisabled(!dosen3Aktif.isChecked());

				dosen4.setDisabled(!dosen4Aktif.isChecked());
				penguji2.setDisabled(!dosen4Aktif.isChecked());

				dosen5.setDisabled(!dosen5Aktif.isChecked());
				penguji3.setDisabled(!dosen5Aktif.isChecked());

				dosen6.setDisabled(!dosen6Aktif.isChecked());
				penguji4.setDisabled(!dosen6Aktif.isChecked());

				dosen7.setDisabled(!dosen7Aktif.isChecked());
				penguji5.setDisabled(!dosen7Aktif.isChecked());

				kode1.setDisabled(!dosen1Aktif.isChecked());
				kode2.setDisabled(!dosen2Aktif.isChecked());
				kode21.setDisabled(!dosen21Aktif.isChecked());

				kode3.setDisabled(!dosen3Aktif.isChecked());
				kode4.setDisabled(!dosen4Aktif.isChecked());
				kode5.setDisabled(!dosen5Aktif.isChecked());
				kode6.setDisabled(!dosen6Aktif.isChecked());
				kode7.setDisabled(!dosen7Aktif.isChecked());
			}
		};

		dosen1Aktif.addEventListener("onClick", s);
		dosen2Aktif.addEventListener("onClick", s);
		dosen21Aktif.addEventListener("onClick", s);
		dosen3Aktif.addEventListener("onClick", s);
		dosen4Aktif.addEventListener("onClick", s);
		dosen5Aktif.addEventListener("onClick", s);
		dosen6Aktif.addEventListener("onClick", s);
		dosen7Aktif.addEventListener("onClick", s);

		try {
			s.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(harusLunas = new MyCheckboxConfig("Mahasiswa Harus Lunas Biaya Semester"));
		harusLunas.setChecked(formatNilaiSkripsi.getHarusLunas());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prosentase Lunas Biaya Semester"));
		row.appendChild(prosentaseLunas = new MyDoublebox(formatNilaiSkripsi.getProsentaseLunas()));
		prosentaseLunas.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Item Biaya"));
		row.appendChild(kodeItemBiaya = new Textbox(formatNilaiSkripsi.getKodeItemBiaya()));
		kodeItemBiaya.setWidth("90%");
		kodeItemBiaya.setRows(2);

		Common.initKeterangan(rows,
				"Jika syarat harus membayar biaya tertentu, masukkan kode item biaya yang harus dibayar mahasiswa. Jika item biaya lebih dari satu, pisahkan dengan tanda koma (,), contoh : 502,505,506 dan seterusnya. Dan juga pastikan kode item biaya benar.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(sekaliBayar = new MyCheckboxConfig(
				"Kode item biaya tersebut sekali bayar saja, jadi kalau misalnya mahasiswa membayar di semester 7, tetap bisa mengajukan di semester 8 atau lebih tanpa membayar ulang."));
		sekaliBayar.setChecked(formatNilaiSkripsi.getSekaliBayar());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(harusMengembalikanBukuPerpustakaan = new MyCheckboxConfig(
				"Mahasiswa Harus Mengembalikan Buku Perpustakaan"));
		harusMengembalikanBukuPerpustakaan.setChecked(formatNilaiSkripsi.getHarusMengembalikanBukuPerpustakaan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(mahasiswaBolehMengubahAgendaAtauJadwalBimbingan = new MyCheckboxConfig(
				"Mahasiswa boleh mengubah agenda atau jadwal bimbingan"));
		mahasiswaBolehMengubahAgendaAtauJadwalBimbingan
				.setChecked(formatNilaiSkripsi.getMahasiswaBolehMengubahAgendaAtauJadwalBimbingan());

		List<TipeItem> tipeItems = HibernateUtil.currentSession().createCriteria(TipeItem.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).list();
		tipeItems.add(null);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelBold("Daftar Lampiran : "));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran1 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran1()));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran1Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran1.setWidth("90%");
		uploadLampiran1Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran1Wajib());
		hbox.appendChild(tipeItem1 = new Combobox());
		Common.insertComboItems(tipeItem1, "nama", "kode", tipeItems);
		tipeItem1.setCols(5);
		tipeItem1.setReadonly(true);
		Common.selectComboItem(tipeItem1,
				formatNilaiSkripsi.getTipeItem1() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem1()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran2 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran2()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran2Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran2.setWidth("90%");
		uploadLampiran2Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran2Wajib());
		hbox.appendChild(tipeItem2 = new Combobox());
		Common.insertComboItems(tipeItem2, "nama", "kode", tipeItems);
		tipeItem2.setCols(5);
		tipeItem2.setReadonly(true);
		Common.selectComboItem(tipeItem2,
				formatNilaiSkripsi.getTipeItem2() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem2()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran3 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran3()));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran3Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran3.setWidth("90%");
		uploadLampiran3Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran3Wajib());

		hbox.appendChild(tipeItem3 = new Combobox());
		Common.insertComboItems(tipeItem3, "nama", "kode", tipeItems);
		tipeItem3.setCols(5);
		tipeItem3.setReadonly(true);
		Common.selectComboItem(tipeItem3,
				formatNilaiSkripsi.getTipeItem3() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem3()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran4 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran4()));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran4Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran4.setWidth("90%");
		uploadLampiran4Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran4Wajib());

		hbox.appendChild(tipeItem4 = new Combobox());
		Common.insertComboItems(tipeItem4, "nama", "kode", tipeItems);
		tipeItem4.setCols(5);
		tipeItem4.setReadonly(true);
		Common.selectComboItem(tipeItem4,
				formatNilaiSkripsi.getTipeItem4() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem4()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran5 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran5()));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran5Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran5.setWidth("90%");
		uploadLampiran5Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran5Wajib());

		hbox.appendChild(tipeItem5 = new Combobox());
		Common.insertComboItems(tipeItem5, "nama", "kode", tipeItems);
		tipeItem5.setCols(5);
		tipeItem5.setReadonly(true);
		Common.selectComboItem(tipeItem5,
				formatNilaiSkripsi.getTipeItem5() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem5()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran6 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran6()));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran6Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran6.setWidth("90%");
		uploadLampiran6Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran6Wajib());

		hbox.appendChild(tipeItem6 = new Combobox());
		Common.insertComboItems(tipeItem6, "nama", "kode", tipeItems);
		tipeItem6.setCols(5);
		tipeItem6.setReadonly(true);
		Common.selectComboItem(tipeItem6,
				formatNilaiSkripsi.getTipeItem6() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem6()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran7 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran7()));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran7Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran7.setWidth("90%");
		uploadLampiran7Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran7Wajib());
		hbox.appendChild(tipeItem7 = new Combobox());
		Common.insertComboItems(tipeItem7, "nama", "kode", tipeItems);
		tipeItem7.setCols(5);
		tipeItem7.setReadonly(true);
		Common.selectComboItem(tipeItem7,
				formatNilaiSkripsi.getTipeItem7() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem7()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran8 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran8()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran8Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran8.setWidth("90%");
		uploadLampiran8Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran8Wajib());

		hbox.appendChild(tipeItem8 = new Combobox());
		Common.insertComboItems(tipeItem8, "nama", "kode", tipeItems);
		tipeItem8.setCols(5);
		tipeItem8.setReadonly(true);
		Common.selectComboItem(tipeItem8,
				formatNilaiSkripsi.getTipeItem8() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem8()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran9 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran9()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran9Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran9.setWidth("90%");
		uploadLampiran9Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran9Wajib());

		hbox.appendChild(tipeItem9 = new Combobox());
		Common.insertComboItems(tipeItem9, "nama", "kode", tipeItems);
		tipeItem9.setCols(5);
		tipeItem9.setReadonly(true);
		Common.selectComboItem(tipeItem9,
				formatNilaiSkripsi.getTipeItem9() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem9()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran10 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran10()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran10Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran10.setWidth("90%");
		uploadLampiran10Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran10Wajib());

		hbox.appendChild(tipeItem10 = new Combobox());
		Common.insertComboItems(tipeItem10, "nama", "kode", tipeItems);
		tipeItem10.setCols(5);
		tipeItem10.setReadonly(true);
		Common.selectComboItem(tipeItem10,
				formatNilaiSkripsi.getTipeItem10() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem10()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran11 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran11()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran11Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran11.setWidth("90%");
		uploadLampiran11Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran11Wajib());

		hbox.appendChild(tipeItem11 = new Combobox());
		Common.insertComboItems(tipeItem11, "nama", "kode", tipeItems);
		tipeItem11.setCols(5);
		tipeItem11.setReadonly(true);
		Common.selectComboItem(tipeItem11,
				formatNilaiSkripsi.getTipeItem11() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem11()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran12 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran12()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran12Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran12.setWidth("90%");
		uploadLampiran12Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran12Wajib());

		hbox.appendChild(tipeItem12 = new Combobox());
		Common.insertComboItems(tipeItem12, "nama", "kode", tipeItems);
		tipeItem12.setCols(5);
		tipeItem12.setReadonly(true);
		Common.selectComboItem(tipeItem12,
				formatNilaiSkripsi.getTipeItem12() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem12()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran13 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran13()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran13Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran13.setWidth("90%");
		uploadLampiran13Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran13Wajib());

		hbox.appendChild(tipeItem13 = new Combobox());
		Common.insertComboItems(tipeItem13, "nama", "kode", tipeItems);
		tipeItem13.setCols(5);
		tipeItem13.setReadonly(true);
		Common.selectComboItem(tipeItem13,
				formatNilaiSkripsi.getTipeItem13() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem13()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran14 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran14()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran14Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran14.setWidth("90%");
		uploadLampiran14Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran14Wajib());

		hbox.appendChild(tipeItem14 = new Combobox());
		Common.insertComboItems(tipeItem14, "nama", "kode", tipeItems);
		tipeItem14.setCols(5);
		tipeItem14.setReadonly(true);
		Common.selectComboItem(tipeItem14,
				formatNilaiSkripsi.getTipeItem14() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem14()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran15 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran15()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran15Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran15.setWidth("90%");
		uploadLampiran15Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran15Wajib());

		hbox.appendChild(tipeItem15 = new Combobox());
		Common.insertComboItems(tipeItem15, "nama", "kode", tipeItems);
		tipeItem15.setCols(5);
		tipeItem15.setReadonly(true);
		Common.selectComboItem(tipeItem15,
				formatNilaiSkripsi.getTipeItem15() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem15()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran16 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran16()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran16Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran16.setWidth("90%");
		uploadLampiran16Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran16Wajib());

		hbox.appendChild(tipeItem16 = new Combobox());
		Common.insertComboItems(tipeItem16, "nama", "kode", tipeItems);
		tipeItem16.setCols(5);
		tipeItem16.setReadonly(true);
		Common.selectComboItem(tipeItem16,
				formatNilaiSkripsi.getTipeItem16() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem16()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran17 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran17()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran17Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran17.setWidth("90%");
		uploadLampiran17Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran17Wajib());

		hbox.appendChild(tipeItem17 = new Combobox());
		Common.insertComboItems(tipeItem17, "nama", "kode", tipeItems);
		tipeItem17.setCols(5);
		tipeItem17.setReadonly(true);
		Common.selectComboItem(tipeItem17,
				formatNilaiSkripsi.getTipeItem17() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem17()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran18 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran18()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran18Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran18.setWidth("90%");
		uploadLampiran18Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran18Wajib());

		hbox.appendChild(tipeItem18 = new Combobox());
		Common.insertComboItems(tipeItem18, "nama", "kode", tipeItems);
		tipeItem18.setCols(5);
		tipeItem18.setReadonly(true);
		Common.selectComboItem(tipeItem18,
				formatNilaiSkripsi.getTipeItem18() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem18()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran19 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran19()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran19Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran19.setWidth("90%");
		uploadLampiran19Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran19Wajib());

		hbox.appendChild(tipeItem19 = new Combobox());
		Common.insertComboItems(tipeItem19, "nama", "kode", tipeItems);
		tipeItem19.setCols(5);
		tipeItem19.setReadonly(true);
		Common.selectComboItem(tipeItem19,
				formatNilaiSkripsi.getTipeItem19() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem19()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran20 = new MyTextbox(formatNilaiSkripsi.getUploadLampiran20()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran20Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran20.setWidth("90%");
		uploadLampiran20Wajib.setChecked(formatNilaiSkripsi.getUploadLampiran20Wajib());

		hbox.appendChild(tipeItem20 = new Combobox());
		Common.insertComboItems(tipeItem20, "nama", "kode", tipeItems);
		tipeItem20.setCols(5);
		tipeItem20.setReadonly(true);
		Common.selectComboItem(tipeItem20,
				formatNilaiSkripsi.getTipeItem20() == null ? null : new TipeItem(formatNilaiSkripsi.getTipeItem20()));

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

		reloadKomponen();
		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadKomponen();
			}
		});
		fakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadKomponen();
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void reloadKomponen() {
		Common.clear(center1);

		MyGrid subGrid = new MyGrid();
		subGrid.setWidth("100%");
		subGrid.setParent(center1);
		subGrid.setHeight("100%");

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		Session session = HibernateUtil.currentSession();
		List<KomponenPenilaianSkripsi> komponenPenilaianSkripsis = session
				.createCriteria(KomponenPenilaianSkripsi.class)
				.add(jurusan == null || jurusan.getSelectedItem() == null
						|| jurusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jurusan"),
										CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false)))

				.add(fakultas == null || fakultas.getSelectedItem() == null
						|| fakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false)))

				.createAlias("parent", "parent", Criteria.LEFT_JOIN).addOrder(Order.asc("parent.nomorUrut"))
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		TreeMap<KomponenPenilaianSkripsi, List<KomponenPenilaianSkripsi>> dataKomponenPenilaian = new TreeMap<KomponenPenilaianSkripsi, List<KomponenPenilaianSkripsi>>();
		for (KomponenPenilaianSkripsi komponenPenilaianSkripsi : komponenPenilaianSkripsis) {
			if (komponenPenilaianSkripsi.getParent() != null) {
				if (!dataKomponenPenilaian.keySet().contains(komponenPenilaianSkripsi.getParent())) {
					List<KomponenPenilaianSkripsi> datas = new ArrayList<KomponenPenilaianSkripsi>();
					datas.add(komponenPenilaianSkripsi);
					dataKomponenPenilaian.put(komponenPenilaianSkripsi.getParent(), datas);
				} else {
					dataKomponenPenilaian.get(komponenPenilaianSkripsi.getParent()).add(komponenPenilaianSkripsi);
				}
			}
		}

		for (KomponenPenilaianSkripsi komponenPenilaianSkripsi : komponenPenilaianSkripsis) {
			if (komponenPenilaianSkripsi.getParent() == null
					&& !dataKomponenPenilaian.containsKey(komponenPenilaianSkripsi)) {
				List<KomponenPenilaianSkripsi> datas = new ArrayList<KomponenPenilaianSkripsi>();
				dataKomponenPenilaian.put(komponenPenilaianSkripsi, datas);
			}
		}

		if (formatNilaiSkripsi.getId() != null) {
			HibernateUtil.currentSession().refresh(this.formatNilaiSkripsi);
		}

		if (formatNilaiSkripsi.getId() != null) {

			selectedKomponenPenilaianSkripsi = session.createCriteria(SkripsiPunyaKomponenPenilaianSkripsi.class)
					.setProjection(Projections.groupProperty("komponenPenilaianSkripsi"))
					.createAlias("komponenPenilaianSkripsi", "komponenPenilaianSkripsi")
					.add(Restrictions.or(Restrictions.isNull("komponenPenilaianSkripsi.aktif"),
							Restrictions.eq("komponenPenilaianSkripsi.aktif", true)))
					.add(Restrictions.eq("formatNilaiSkripsi", formatNilaiSkripsi)).list();

		} else {
			selectedKomponenPenilaianSkripsi = new ArrayList<KomponenPenilaianSkripsi>();
		}

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);

		for (final KomponenPenilaianSkripsi parent : dataKomponenPenilaian.keySet()) {

			final List<KomponenPenilaianSkripsi> datas = dataKomponenPenilaian.get(parent);
			if (datas.isEmpty()) {
				final Checkbox checkbox = new Checkbox(parent.getNama());
				checkbox.setParent(vboxSkala);
				checkbox.setChecked(selectedKomponenPenilaianSkripsi.contains(parent));
				checkbox.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkbox.isChecked()) {
							selectedKomponenPenilaianSkripsi.add(parent);
						} else {
							selectedKomponenPenilaianSkripsi.remove(parent);
						}
					}
				});
			} else {

				final Checkbox checkboxAll = new Checkbox(parent.getNama());
				checkboxAll.setParent(vboxSkala);
				checkboxAll.setChecked(selectedKomponenPenilaianSkripsi.contains(parent));

				Hbox myHb = new Hbox();
				myHb.setParent(vboxSkala);

				myHb.appendChild(new Space());

				Vbox vboxSkalaSub = new Vbox();
				vboxSkalaSub.setPack("top");
				vboxSkalaSub.setParent(myHb);

				final List<Checkbox> checkboxs = new ArrayList<Checkbox>();
				for (final KomponenPenilaianSkripsi komponenPenilaianSkripsi : datas) {

					final Checkbox checkbox = new Checkbox(komponenPenilaianSkripsi.getNama());
					checkboxs.add(checkbox);
					checkbox.setParent(vboxSkalaSub);
					checkbox.setChecked(selectedKomponenPenilaianSkripsi.contains(komponenPenilaianSkripsi));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (checkbox.isChecked()) {
								selectedKomponenPenilaianSkripsi.add(komponenPenilaianSkripsi);
							} else {
								selectedKomponenPenilaianSkripsi.remove(komponenPenilaianSkripsi);
							}
						}
					});

				}

				checkboxAll.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (checkboxAll.isChecked()) {
							selectedKomponenPenilaianSkripsi.add(parent);
						} else {
							selectedKomponenPenilaianSkripsi.remove(parent);
						}

						for (Checkbox checkbox : checkboxs) {
							checkbox.setChecked(checkboxAll.isChecked());
						}
						for (final KomponenPenilaianSkripsi komponenPenilaianSkripsi : datas) {
							if (checkboxAll.isChecked()) {
								selectedKomponenPenilaianSkripsi.add(komponenPenilaianSkripsi);
							} else {
								selectedKomponenPenilaianSkripsi.remove(komponenPenilaianSkripsi);
							}
						}
					}
				});

			}
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (ketuaSidang.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Prosentase Nilai Pembimbing 1",
					"Kolom Prosentase Nilai Pembimbing 1 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Prosentase Nilai Pembimbing 1.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			ketuaSidang.focus();
			return false;
		}
		if (pembimbing1.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Prosentase Nilai Pembimbing 2",
					"Kolom Prosentase Nilai Pembimbing 2 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Prosentase Nilai Pembimbing 2.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			pembimbing1.focus();
			return false;
		}
		if (prosentasiNilaiPembimbing3.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Prosentase Nilai Pembimbing 3",
					"Kolom Prosentase Nilai Pembimbing 3 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Prosentase Nilai Pembimbing 3.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			prosentasiNilaiPembimbing3.focus();
			return false;
		}
		if (penguji1.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Prosentase Nilai Penguji 1",
					"Kolom Prosentase Nilai Penguji 1 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Prosentase Nilai Penguji 1.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			penguji1.focus();
			return false;
		}

		if (penguji2.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Prosentase Nilai Penguji 2",
					"Kolom Prosentase Nilai Penguji 2 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Prosentase Nilai Penguji 2.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			penguji2.focus();
			return false;
		}

		if (penguji3.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Prosentase Nilai Penguji 3",
					"Kolom Prosentase Nilai Penguji 3 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Prosentase Nilai Penguji 3.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			penguji3.focus();
			return false;
		}

		if (penguji4.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Prosentase Nilai Penguji 4",
					"Kolom Prosentase Nilai Penguji 4 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Prosentase Nilai Penguji 4.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			penguji4.focus();
			return false;
		}

		if (penguji5.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Prosentase Nilai Penguji 5",
					"Kolom Prosentase Nilai Penguji 5 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Prosentase Nilai Penguji 5.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			penguji5.focus();
			return false;
		}

		Double temp_total;
		temp_total = ketuaSidang.getValue() + pembimbing1.getValue() + prosentasiNilaiPembimbing3.getValue()
				+ penguji1.getValue() + penguji2.getValue() + penguji3.getValue() + penguji4.getValue()
				+ penguji5.getValue();
		System.out.println("total : " + temp_total);
		if (temp_total.intValue() < 0) {
			MyMessageboxConfig.show("Jumlah Format Nilai tidak boleh negatif", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (temp_total.intValue() > 100) {
			MyMessageboxConfig.show("Jumlah Format Nilai tidak boleh lebih besar dari 100", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (temp_total.intValue() != 100) {
			MyMessageboxConfig.show("Jumlah Format Nilai harus berjumlah 100", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (temp_total.intValue() < 100) {
			MyMessageboxConfig.show("Jumlah Format Nilai harus berjumlah 100", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		FormatNilaiSkripsiDao formatNilaiSkripsiDao = DaoFactory.getInstance().getFormatNilaiSkripsiDao();
		if (formatNilaiSkripsi.getId() != null) {
			formatNilaiSkripsi = formatNilaiSkripsiDao.load(formatNilaiSkripsi.getId());
		}
		formatNilaiSkripsi.setTidakBolehDipilihMahasiswa(tidakBolehDipilihMahasiswa.isChecked());
		formatNilaiSkripsi.setNama(nama.getValue());
		formatNilaiSkripsi.setProsentasiNilaiKetuaSidang(ketuaSidang.getValue());
		formatNilaiSkripsi.setProsentasiNilaiPembimbing(pembimbing1.getValue());
		formatNilaiSkripsi.setProsentasiNilaiPembimbing3(prosentasiNilaiPembimbing3.getValue());
		formatNilaiSkripsi.setProsentasiNilaiPenguji1(penguji1.getValue());
		formatNilaiSkripsi.setProsentasiNilaiPenguji2(penguji2.getValue());
		formatNilaiSkripsi.setProsentasiNilaiPenguji3(penguji3.getValue());
		formatNilaiSkripsi.setProsentasiNilaiPenguji4(penguji4.getValue());
		formatNilaiSkripsi.setProsentasiNilaiPenguji5(penguji5.getValue());
		formatNilaiSkripsi.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		formatNilaiSkripsi.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));

		formatNilaiSkripsi.setJenisKegiatanMahasiswa(
				(JenisKegiatanMahasiswa) (jenis.getSelectedItem() == null ? null : jenis.getSelectedItem().getValue()));

		formatNilaiSkripsi.setDosen1(dosen1.getValue());
		formatNilaiSkripsi.setDosen2(dosen2.getValue());
		formatNilaiSkripsi.setDosen21(dosen21.getValue());
		formatNilaiSkripsi.setDosen3(dosen3.getValue());
		formatNilaiSkripsi.setDosen4(dosen4.getValue());
		formatNilaiSkripsi.setDosen5(dosen5.getValue());
		formatNilaiSkripsi.setDosen6(dosen6.getValue());
		formatNilaiSkripsi.setDosen7(dosen7.getValue());

		formatNilaiSkripsi.setMinimalIpk(minimalIpk.getValue());
		formatNilaiSkripsi.setMinimalSks(minimalSks.getValue());
		String kodeMatakuliahAtau = kodeMatakuliah.getValue().trim();
		formatNilaiSkripsi.setKodeMatakuliah(kodeMatakuliahAtau);
		formatNilaiSkripsi.setMinimalAngkaKredit(minimalAngkaKredit.getValue());
		formatNilaiSkripsi.setMatkulPrasyaratLulus(
				matkulPrasyaratLulus.getValue() == null ? null : matkulPrasyaratLulus.getValue().trim());
		formatNilaiSkripsi.setHarusLunas(harusLunas.isChecked());
		formatNilaiSkripsi.setProsentaseLunas(prosentaseLunas.getValue());
		formatNilaiSkripsi.setHarusMengembalikanBukuPerpustakaan(harusMengembalikanBukuPerpustakaan.isChecked());
		formatNilaiSkripsi.setKodeItemBiaya(kodeItemBiaya.getValue());
		formatNilaiSkripsi.setBobot(bobot.getValue());
		formatNilaiSkripsi.setTidakWajibMengambilMkTertentu(tidakWajibMengambilMkTertentu.isChecked());

		formatNilaiSkripsi.setUploadLampiran1(uploadLampiran1.getValue());
		formatNilaiSkripsi.setUploadLampiran2(uploadLampiran2.getValue());
		formatNilaiSkripsi.setUploadLampiran3(uploadLampiran3.getValue());
		formatNilaiSkripsi.setUploadLampiran4(uploadLampiran4.getValue());
		formatNilaiSkripsi.setUploadLampiran5(uploadLampiran5.getValue());
		formatNilaiSkripsi.setUploadLampiran6(uploadLampiran6.getValue());
		formatNilaiSkripsi.setUploadLampiran7(uploadLampiran7.getValue());
		formatNilaiSkripsi.setUploadLampiran8(uploadLampiran8.getValue());
		formatNilaiSkripsi.setUploadLampiran9(uploadLampiran9.getValue());
		formatNilaiSkripsi.setUploadLampiran10(uploadLampiran10.getValue());

		formatNilaiSkripsi.setUploadLampiran11(uploadLampiran11.getValue());
		formatNilaiSkripsi.setUploadLampiran12(uploadLampiran12.getValue());
		formatNilaiSkripsi.setUploadLampiran13(uploadLampiran13.getValue());
		formatNilaiSkripsi.setUploadLampiran14(uploadLampiran14.getValue());
		formatNilaiSkripsi.setUploadLampiran15(uploadLampiran15.getValue());

		formatNilaiSkripsi.setUploadLampiran1Wajib(uploadLampiran1Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran2Wajib(uploadLampiran2Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran3Wajib(uploadLampiran3Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran4Wajib(uploadLampiran4Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran5Wajib(uploadLampiran5Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran6Wajib(uploadLampiran6Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran7Wajib(uploadLampiran7Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran8Wajib(uploadLampiran8Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran9Wajib(uploadLampiran9Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran10Wajib(uploadLampiran10Wajib.isChecked());

		formatNilaiSkripsi.setUploadLampiran11Wajib(uploadLampiran11Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran12Wajib(uploadLampiran12Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran13Wajib(uploadLampiran13Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran14Wajib(uploadLampiran14Wajib.isChecked());
		formatNilaiSkripsi.setUploadLampiran15Wajib(uploadLampiran15Wajib.isChecked());

		formatNilaiSkripsi.setKodeMatakuliahDan(KrsDanSkripsiHelper.kodeMatakuliahDanEfektif(
				kodeMatakuliahDan.getValue(), kodeMatakuliahAtau));

		formatNilaiSkripsi.setTipeItem1(
				tipeItem1.getSelectedItem() == null || tipeItem1.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem1.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem2(
				tipeItem2.getSelectedItem() == null || tipeItem2.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem2.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem3(
				tipeItem3.getSelectedItem() == null || tipeItem3.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem3.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem4(
				tipeItem4.getSelectedItem() == null || tipeItem4.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem4.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem5(
				tipeItem5.getSelectedItem() == null || tipeItem5.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem5.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem6(
				tipeItem6.getSelectedItem() == null || tipeItem6.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem6.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem7(
				tipeItem7.getSelectedItem() == null || tipeItem7.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem7.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem8(
				tipeItem8.getSelectedItem() == null || tipeItem8.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem8.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem9(
				tipeItem9.getSelectedItem() == null || tipeItem9.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem9.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem10(
				tipeItem10.getSelectedItem() == null || tipeItem10.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem10.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem11(
				tipeItem11.getSelectedItem() == null || tipeItem11.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem11.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem12(
				tipeItem12.getSelectedItem() == null || tipeItem12.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem12.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem13(
				tipeItem13.getSelectedItem() == null || tipeItem13.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem13.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem14(
				tipeItem14.getSelectedItem() == null || tipeItem14.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem14.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTipeItem15(
				tipeItem15.getSelectedItem() == null || tipeItem15.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem15.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setTahunAngkatan(tahunAngkatan.getValue());
		formatNilaiSkripsi.setSekaliBayar(sekaliBayar.isChecked());

		formatNilaiSkripsi.setUploadLampiran16(uploadLampiran16.getValue());
		formatNilaiSkripsi.setUploadLampiran16Wajib(uploadLampiran16Wajib.isChecked());
		formatNilaiSkripsi.setTipeItem16(
				tipeItem16.getSelectedItem() == null || tipeItem16.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem16.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setUploadLampiran17(uploadLampiran17.getValue());
		formatNilaiSkripsi.setUploadLampiran17Wajib(uploadLampiran17Wajib.isChecked());
		formatNilaiSkripsi.setTipeItem17(
				tipeItem17.getSelectedItem() == null || tipeItem17.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem17.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setUploadLampiran18(uploadLampiran18.getValue());
		formatNilaiSkripsi.setUploadLampiran18Wajib(uploadLampiran18Wajib.isChecked());
		formatNilaiSkripsi.setTipeItem18(
				tipeItem18.getSelectedItem() == null || tipeItem18.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem18.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setUploadLampiran19(uploadLampiran19.getValue());
		formatNilaiSkripsi.setUploadLampiran19Wajib(uploadLampiran19Wajib.isChecked());
		formatNilaiSkripsi.setTipeItem19(
				tipeItem19.getSelectedItem() == null || tipeItem19.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem19.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setUploadLampiran20(uploadLampiran20.getValue());
		formatNilaiSkripsi.setUploadLampiran20Wajib(uploadLampiran20Wajib.isChecked());
		formatNilaiSkripsi.setTipeItem20(
				tipeItem20.getSelectedItem() == null || tipeItem20.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem20.getSelectedItem().getValue()).getId());

		formatNilaiSkripsi.setDosen1Aktif(dosen1Aktif.isChecked());
		formatNilaiSkripsi.setDosen2Aktif(dosen2Aktif.isChecked());
		formatNilaiSkripsi.setDosen21Aktif(dosen21Aktif.isChecked());
		formatNilaiSkripsi.setDosen3Aktif(dosen3Aktif.isChecked());
		formatNilaiSkripsi.setDosen4Aktif(dosen4Aktif.isChecked());
		formatNilaiSkripsi.setDosen5Aktif(dosen5Aktif.isChecked());
		formatNilaiSkripsi.setDosen6Aktif(dosen6Aktif.isChecked());
		formatNilaiSkripsi.setDosen7Aktif(dosen7Aktif.isChecked());

		formatNilaiSkripsi.setKode1(kode1.getValue().trim());
		formatNilaiSkripsi.setKode2(kode2.getValue().trim());
		formatNilaiSkripsi.setKode21(kode21.getValue().trim());
		formatNilaiSkripsi.setKode3(kode3.getValue().trim());
		formatNilaiSkripsi.setKode4(kode4.getValue().trim());
		formatNilaiSkripsi.setKode5(kode5.getValue().trim());
		formatNilaiSkripsi.setKode6(kode6.getValue().trim());
		formatNilaiSkripsi.setKode7(kode7.getValue().trim());

		formatNilaiSkripsi
				.setJenisNilaiHuruf((JenisNilaiHurufMatakuliah) (jenisNilaiHuruf.getSelectedItem() == null ? null
						: jenisNilaiHuruf.getSelectedItem().getValue()));

		formatNilaiSkripsi.setMahasiswaBolehMengubahAgendaAtauJadwalBimbingan(
				mahasiswaBolehMengubahAgendaAtauJadwalBimbingan.isChecked());

		Common.refreshSaveOrUpdate(formatNilaiSkripsi);

		Session session = HibernateUtil.currentSession();
		session.createSQLQuery("delete from skripsi_punya_komponen_penilaian_skripsi where format_nilai_skripsi="
				+ formatNilaiSkripsi.getId()).executeUpdate();
		for (KomponenPenilaianSkripsi komponenPenilaianSkripsi : selectedKomponenPenilaianSkripsi) {
			SkripsiPunyaKomponenPenilaianSkripsi skripsiPunyaKomponenPenilaianSkripsi = new SkripsiPunyaKomponenPenilaianSkripsi();
			skripsiPunyaKomponenPenilaianSkripsi.setKomponenPenilaianSkripsi(komponenPenilaianSkripsi);
			skripsiPunyaKomponenPenilaianSkripsi.setNama(komponenPenilaianSkripsi.getNama());
			skripsiPunyaKomponenPenilaianSkripsi.setFormatNilaiSkripsi(formatNilaiSkripsi);
			session.save(skripsiPunyaKomponenPenilaianSkripsi);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(FormatNilaiSkripsi.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.asc("nama"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<FormatNilaiSkripsi> formatNilaiSkripsi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(formatNilaiSkripsi);
		grid.setRowRenderer(new FormatNilaiSkripsiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkBobot() {

		Integer bobotCount = null;
		Session session = HibernateUtil.currentSession();
		bobotCount = ((Number) session.createCriteria(FormatNilaiSkripsi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("prosentasiNilaiKetuaSidang", ketuaSidang.getValue()))
				.add(Restrictions.eq("prosentasiNilaiPembimbing", pembimbing1.getValue()))
				.add(Restrictions.eq("prosentasiNilaiPenguji1", penguji1.getValue()))
				.add(Restrictions.eq("prosentasiNilaiPenguji2", penguji2.getValue()))
				.add(Restrictions.eq("prosentasiNilaiPenguji3", penguji3.getValue()))
				.add(Restrictions.eq("prosentasiNilaiPenguji4", penguji4.getValue()))
				.add(this.formatNilaiSkripsi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.formatNilaiSkripsi.getId()))
				.uniqueResult()).intValue();

		return !bobotCount.equals(0);
	}

}
