package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.GuruMengajar;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JamPelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.SubMatapelajaran;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk guru mengajar. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchhari}, {@code Textbox
 * searchketerangan}, {@code Combobox searchyayasan}, {@code Combobox searchsekolah}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onCopyJadwalPelajaran()}, {@code onAddExternal()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
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
public class GuruMengajarAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	protected Combobox searchhari;
	protected Textbox searchketerangan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private AmbilDataGuruBanbox searchguru;
	private AmbilDataGuruBanbox guru;
	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private GuruMengajar guruMengajar;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Combobox matapelajaran;
	private Combobox jamPelajaran;
	private Combobox hari;

	private Combobox hari2;
	private Combobox jamPelajaran2;
	private Combobox hari3;
	private Combobox jamPelajaran3;
	private Combobox hari4;
	private Combobox jamPelajaran4;
	private Combobox hari5;
	private Combobox jamPelajaran5;

	private Tbmuser tbmuser;
	private Combobox hari6;
	private Combobox jamPelajaran6;
	private Combobox hari7;
	private Combobox jamPelajaran7;
	private Combobox hari8;
	private Combobox jamPelajaran8;
	private Combobox hari9;
	private Combobox jamPelajaran9;
	private Combobox hari10;
	private Combobox jamPelajaran10;
	private Combobox hari11;
	private Combobox hari12;
	private Combobox jamPelajaran11;
	private Combobox jamPelajaran12;
	private EventListener eventListener = null;

	private Combobox subMatapelajaran;
	private Combobox subMatapelajaran2;
	private Combobox subMatapelajaran3;
	private Combobox subMatapelajaran4;
	private Combobox subMatapelajaran5;
	private Combobox subMatapelajaran6;
	private Combobox subMatapelajaran7;
	private Combobox subMatapelajaran8;
	private Combobox subMatapelajaran9;
	private Combobox subMatapelajaran10;
	private Combobox subMatapelajaran11;
	private Combobox subMatapelajaran12;

	private Guru guruSelected = null;
	private Combobox hari13;
	private Combobox jamPelajaran13;
	private Combobox subMatapelajaran13;
	private Combobox hari14;
	private Combobox jamPelajaran14;
	private Combobox subMatapelajaran14;
	private Combobox hari15;
	private Combobox jamPelajaran15;
	private Combobox subMatapelajaran15;
	private Combobox hari16;
	private Combobox jamPelajaran16;
	private Combobox subMatapelajaran16;
	private Combobox hari17;
	private Combobox jamPelajaran17;
	private Combobox subMatapelajaran17;
	private Combobox hari18;
	private Combobox jamPelajaran18;
	private Combobox subMatapelajaran18;
	private Combobox hari19;
	private Combobox jamPelajaran19;
	private Combobox subMatapelajaran19;
	private Combobox hari20;
	private Combobox jamPelajaran20;
	private Combobox subMatapelajaran20;
	private Combobox hari21;
	private Combobox jamPelajaran21;
	private Combobox subMatapelajaran21;
	private Combobox hari22;
	private Combobox jamPelajaran22;
	private Combobox subMatapelajaran22;
	private Combobox hari23;
	private Combobox jamPelajaran23;
	private Combobox subMatapelajaran23;
	private Combobox hari24;
	private Combobox jamPelajaran24;
	private Combobox subMatapelajaran24;
	private Combobox hari25;
	private Combobox jamPelajaran25;
	private Combobox subMatapelajaran25;

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

		MyComboitemConfig comboitem;
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			searchhari.appendChild(comboitem);
		}
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchhari.appendChild(comboitem);
		if (searchhari != null) { searchhari.setReadonly(true); }
		if (searchhari != null) { searchhari.setSelectedItem(comboitem); }

		tbmuser = Common.getCurrentUser();

		if (execution.getParameter("guru_selected") != null) {
			guruSelected = (Guru) ConstantValues.ambil(Guru.class.getName(),
					Long.parseLong(execution.getParameter("guru_selected")));

			if (guruSelected != null) {
				searchguru.setAttribute("guru", guruSelected);
				searchguru.setValue(guruSelected == null ? "" : guruSelected.getNama());
				searchguru.setDisabled(true);
			}
		}

		searchguru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

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

		String[] contents = new String[] { "id", "matapelajaran", "abaikanBentrok", "hari",

				"hari2", "hari3", "hari4", "hari5", "hari6", "hari7", "hari8", "hari9", "hari10", "hari11", "hari12",

				"jamPelajaran",

				"jamPelajaran2", "jamPelajaran3", "jamPelajaran4", "jamPelajaran5", "jamPelajaran6", "jamPelajaran7",
				"jamPelajaran8", "jamPelajaran9", "jamPelajaran10", "jamPelajaran11", "jamPelajaran12", "guru",

				"sekolah", "keterangan"

				, "subMatapelajaran",

				"subMatapelajaran2", "subMatapelajaran3", "subMatapelajaran4", "subMatapelajaran5", "subMatapelajaran6",
				"subMatapelajaran7", "subMatapelajaran8", "subMatapelajaran9", "subMatapelajaran10",
				"subMatapelajaran11", "subMatapelajaran12"

		};
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, GuruMengajar.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

	        FilterLanjutHelper.setup(comp);
}

	public void onCopyJadwalPelajaran(Event event) throws Exception {
		final MyWindow window = new MyWindow("Copy Jadwal Pelajaran", "normal", true);
		page.getFirstRoot().appendChild(window);
		window.setHeight("300px");
		window.setWidth("400px");

		final Combobox tahunAkademikDari = Common.generateTahunAjaran(null);

		final Combobox jenisSemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(JadwalPelajaran.GANJIL);
		comboitem.setValue(JadwalPelajaran.GANJIL);
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JadwalPelajaran.GENAP);
		comboitem.setValue(JadwalPelajaran.GENAP);
		jenisSemester.appendChild(comboitem);

		Common.selectComboItem(jenisSemester,
				Common.isNowSemensterGanjil() ? JadwalPelajaran.GANJIL : JadwalPelajaran.GENAP);

		final Combobox yayasan = new Combobox();
		final Combobox sekolah = new Combobox();

		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		// grid.setOddRowSclass("non-odd");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran"));
		row.appendChild(tahunAkademikDari);
		tahunAkademikDari.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Copy dari Jadwal Pelajaran", "/img/svg/edit-copy.svg");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String tahunAkademik1 = (String) (tahunAkademikDari.getSelectedItem() == null ? null
						: tahunAkademikDari.getSelectedItem().getValue());

				final String semester1 = (String) (jenisSemester.getSelectedItem() == null ? null
						: jenisSemester.getSelectedItem().getValue());

				final Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
						|| sekolah.getSelectedItem().getValue() == null ? null : sekolah.getSelectedItem().getValue());
				final Yayasan myYayasan = (Yayasan) (yayasan.getSelectedItem() == null
						|| yayasan.getSelectedItem().getValue() == null ? null : yayasan.getSelectedItem().getValue());

				if (tahunAkademik1 == null) {
					MyMessageboxConfig.show("Tahun Ajaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (mySekolah == null) {
					MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (semester1 == null) {
					MyMessageboxConfig.show("Jenis Semester harus diisi", "Peringatan", 1,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();
				@SuppressWarnings("unchecked")
				final List<JadwalPelajaran> jadwalPelajarans = ConstantValues
						.simpleList(session.createCriteria(JadwalPelajaran.class)

								.add(myYayasan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("yayasan", myYayasan))

								.add(mySekolah == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("sekolah", mySekolah))

								.add(Restrictions.eq("tahunAjaran", tahunAkademik1))
								.add(Restrictions.sqlRestriction(
										semester1.equals(JadwalPelajaran.GANJIL) ? "this_.semester % 2 = 1"
												: "this_.semester % 2 = 0")),
								JadwalPelajaran.class);

				System.out.println("jadwalPelajarans -> " + jadwalPelajarans.size());

				MyMessageboxConfig.show(
						"Apakah yakin ingin melanjutkan men-copy " + jadwalPelajarans.size()
								+ " jadwal pelajaran dari tahun ajaran " + tahunAkademik1 + " semester " + semester1
								+ " ke guru mengajar ?",
						"Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									String warning = "";
									Session session = HibernateUtil.currentSession();
									for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {

										List<Guru> gurus = jadwalPelajaran.populateGuruBuNama();
										if (!gurus.isEmpty()) {

											List<Object[]> jamJadwal = jadwalPelajaran.populateJamPelajaran();
											for (Guru guru : gurus) {
												GuruMengajar guruMengajar = (GuruMengajar) session
														.createCriteria(GuruMengajar.class)
														.add(Restrictions.eq("guru", guru))
														.add(Restrictions.eq("matapelajaran",
																jadwalPelajaran.getMatapelajaran()))
														.setMaxResults(1).uniqueResult();

												if (guruMengajar == null) {
													guruMengajar = new GuruMengajar();
													guruMengajar.setGuru(guru);
													guruMengajar.setMatapelajaran(jadwalPelajaran.getMatapelajaran());
												}

												List<Object[]> jam = guruMengajar.populateJamPelajaran();
												List<Object[]> tidakAda = new ArrayList<Object[]>();
												for (Object[] jadwal1 : jamJadwal) {
													JamPelajaran jamPelajaran1 = (JamPelajaran) jadwal1[0];
													String hari1 = (String) jadwal1[1];
													boolean adaData = false;
													for (Object[] jadwal2 : jam) {
														JamPelajaran jamPelajaran2 = (JamPelajaran) jadwal2[0];
														String hari2 = (String) jadwal2[1];

														if (jamPelajaran2.getId().equals(jamPelajaran1.getId())
																&& hari1.equals(hari2)) {
															adaData = true;
															break;
														}
													}

													if (!adaData) {
														tidakAda.add(jadwal1);
													}
												}
												System.out.println(
														"guru -> " + guru + ", tidakAda " + tidakAda.size() + " ");

												for (Object[] jadwal : tidakAda) {
													JamPelajaran jamPelajaran = (JamPelajaran) jadwal[0];
													String hari = (String) jadwal[1];
													if (guruMengajar.getJamPelajaran() == null) {
														guruMengajar.setJamPelajaran(jamPelajaran);
														guruMengajar.setHari(hari);
													} else if (guruMengajar.getJamPelajaran2() == null) {
														guruMengajar.setJamPelajaran2(jamPelajaran);
														guruMengajar.setHari2(hari);
													} else if (guruMengajar.getJamPelajaran3() == null) {
														guruMengajar.setJamPelajaran3(jamPelajaran);
														guruMengajar.setHari3(hari);
													} else if (guruMengajar.getJamPelajaran4() == null) {
														guruMengajar.setJamPelajaran4(jamPelajaran);
														guruMengajar.setHari4(hari);
													} else if (guruMengajar.getJamPelajaran5() == null) {
														guruMengajar.setJamPelajaran5(jamPelajaran);
														guruMengajar.setHari5(hari);
													} else if (guruMengajar.getJamPelajaran6() == null) {
														guruMengajar.setJamPelajaran6(jamPelajaran);
														guruMengajar.setHari6(hari);
													} else if (guruMengajar.getJamPelajaran7() == null) {
														guruMengajar.setJamPelajaran7(jamPelajaran);
														guruMengajar.setHari7(hari);
													} else if (guruMengajar.getJamPelajaran8() == null) {
														guruMengajar.setJamPelajaran8(jamPelajaran);
														guruMengajar.setHari8(hari);
													} else if (guruMengajar.getJamPelajaran9() == null) {
														guruMengajar.setJamPelajaran9(jamPelajaran);
														guruMengajar.setHari9(hari);
													} else if (guruMengajar.getJamPelajaran10() == null) {
														guruMengajar.setJamPelajaran10(jamPelajaran);
														guruMengajar.setHari10(hari);
													} else if (guruMengajar.getJamPelajaran11() == null) {
														guruMengajar.setJamPelajaran11(jamPelajaran);
														guruMengajar.setHari11(hari);
													} else if (guruMengajar.getJamPelajaran12() == null) {
														guruMengajar.setJamPelajaran12(jamPelajaran);
														guruMengajar.setHari12(hari);
													} else if (guruMengajar.getJamPelajaran13() == null) {
														guruMengajar.setJamPelajaran13(jamPelajaran);
														guruMengajar.setHari13(hari);
													} else if (guruMengajar.getJamPelajaran14() == null) {
														guruMengajar.setJamPelajaran14(jamPelajaran);
														guruMengajar.setHari14(hari);
													} else if (guruMengajar.getJamPelajaran15() == null) {
														guruMengajar.setJamPelajaran15(jamPelajaran);
														guruMengajar.setHari15(hari);
													} else if (guruMengajar.getJamPelajaran16() == null) {
														guruMengajar.setJamPelajaran16(jamPelajaran);
														guruMengajar.setHari16(hari);
													} else if (guruMengajar.getJamPelajaran17() == null) {
														guruMengajar.setJamPelajaran17(jamPelajaran);
														guruMengajar.setHari17(hari);
													} else if (guruMengajar.getJamPelajaran18() == null) {
														guruMengajar.setJamPelajaran18(jamPelajaran);
														guruMengajar.setHari18(hari);
													} else if (guruMengajar.getJamPelajaran19() == null) {
														guruMengajar.setJamPelajaran19(jamPelajaran);
														guruMengajar.setHari19(hari);
													} else if (guruMengajar.getJamPelajaran20() == null) {
														guruMengajar.setJamPelajaran20(jamPelajaran);
														guruMengajar.setHari20(hari);
													} else if (guruMengajar.getJamPelajaran21() == null) {
														guruMengajar.setJamPelajaran21(jamPelajaran);
														guruMengajar.setHari21(hari);
													} else if (guruMengajar.getJamPelajaran22() == null) {
														guruMengajar.setJamPelajaran22(jamPelajaran);
														guruMengajar.setHari22(hari);
													} else if (guruMengajar.getJamPelajaran23() == null) {
														guruMengajar.setJamPelajaran23(jamPelajaran);
														guruMengajar.setHari23(hari);
													} else if (guruMengajar.getJamPelajaran24() == null) {
														guruMengajar.setJamPelajaran24(jamPelajaran);
														guruMengajar.setHari24(hari);
													} else if (guruMengajar.getJamPelajaran25() == null) {
														guruMengajar.setJamPelajaran25(jamPelajaran);
														guruMengajar.setHari25(hari);
													}
												}

												if (guruMengajar.getId() == null) {
													session.save(guruMengajar);
													session.flush();
												} else {
													Common.refreshUpdate(session, guruMengajar);
												}
											}
										}

									}
									MyMessageboxConfig.show("Copy jadwal pelajaran telah selesai dilakukan" + warning,
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													window.detach();
													onSearchDefault(arg0);
												}
											});

								}

							}
						});

			}
		});

		save.setParent(toolbar);

		window.onModal();
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link GuruMengajarAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link GuruMengajarAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see GuruMengajarAction
	 */
	class GuruMengajarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			GuruMengajar guruMengajar = (GuruMengajar) arg1;

			RevisiHelper
					.createNewRevisi(GuruMengajar.class, guruMengajar,
							guruMengajar.getMatapelajaran() == null ? "" : guruMengajar.getMatapelajaran().getNama())
					.setParent(arg0);

			new Label(guruMengajar.getGuru() == null ? "" : guruMengajar.getGuru().getNama()).setParent(arg0);
			new Label(guruMengajar.infoSimple()).setParent(arg0);
			new Label(guruMengajar.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, edit, delete, guruMengajar, GuruMengajarAction.this, true)
					.setParent(arg0);

		}

	}

	public static void onAddExternal(EventListener eventListener, GuruMengajar guruMengajar) throws Exception {
		GuruMengajarAction skripsiAction = new GuruMengajarAction();
		skripsiAction.tbmuser = Common.getCurrentUser();
		skripsiAction.eventListener = eventListener;
		skripsiAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(skripsiAction.addWindow);
		skripsiAction.addWindow.setHeight("95%");
		skripsiAction.addWindow.setWidth("750px");

		skripsiAction.init(guruMengajar);

		Common.freezeGanti(skripsiAction.matapelajaran);

		skripsiAction.addWindow.setVisible(true);
		skripsiAction.addWindow.setClosable(true);
		skripsiAction.addWindow.onModal();

	}

	public void onAdd(Event event) throws Exception {

		GuruMengajar guruMengajar = new GuruMengajar();

		init(guruMengajar);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		guruMengajar = (GuruMengajar) obj;
		init(guruMengajar);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final GuruMengajar guruMengajar) throws Exception {
		this.guruMengajar = guruMengajar;
		addWindow.setTitle(guruMengajar.getId() == null ? "Tambah Guru Mengajar" : "Ubah Guru Mengajar");
		addWindow.setWidth("750px");
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

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, guruMengajar.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, guruMengajar.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mata Pelajaran *"));
		row.appendChild(matapelajaran = new Combobox());
		matapelajaran.setWidth("90%");
		matapelajaran.setReadonly(true);
		Common.selectComboItem(true, matapelajaran, guruMengajar.getMatapelajaran());

		if (guruSelected != null) {
			guruMengajar.setGuru(guruSelected);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru *"));
		row.appendChild(guru = new AmbilDataGuruBanbox());
		guru.setWidth("90%");
		guru.setReadonly(true);
		guru.setAttribute("guru", guruMengajar.getGuru());
		guru.setValue(guruMengajar.getGuru() == null ? "" : guruMengajar.getGuru().getNama());

		if (guruSelected != null) {
			guru.setDisabled(true);
		}

		List<String> hrs = guruMengajar.populateHari();

		row = new MyFormRow();
		row.setVisible(hrs.size() == 0);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Toolbarbutton toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran I"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hari = new Combobox();
		MyComboitemConfig comboitem;
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari.appendChild(comboitem);

		Common.selectComboItem(true, hari, guruMengajar.getHari());

		hbox.appendChild(hari);

		hari.setCols(5);
		hari.setReadonly(true);

		jamPelajaran = new Combobox();
		hbox.appendChild(jamPelajaran);

		jamPelajaran.setCols(12);
		jamPelajaran.setReadonly(true);

		subMatapelajaran = new Combobox();
		hbox.appendChild(subMatapelajaran);

		subMatapelajaran.setCols(12);
		subMatapelajaran.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 1);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari2.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari2.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari2() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran II"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari2 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari2.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari2.appendChild(comboitem);

		Common.selectComboItem(true, hari2, guruMengajar.getHari2());

		hbox.appendChild(hari2);

		hari2.setCols(5);
		hari2.setReadonly(true);

		jamPelajaran2 = new Combobox();
		hbox.appendChild(jamPelajaran2);

		jamPelajaran2.setCols(12);
		jamPelajaran2.setReadonly(true);

		subMatapelajaran2 = new Combobox();
		hbox.appendChild(subMatapelajaran2);

		subMatapelajaran2.setCols(12);
		subMatapelajaran2.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 2);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari3.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari3.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari3() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran III"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari3 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari3.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari3.appendChild(comboitem);

		Common.selectComboItem(true, hari3, guruMengajar.getHari3());

		hbox.appendChild(hari3);

		hari3.setCols(5);
		hari3.setReadonly(true);

		jamPelajaran3 = new Combobox();
		hbox.appendChild(jamPelajaran3);
		jamPelajaran3.setCols(12);
		jamPelajaran3.setReadonly(true);

		subMatapelajaran3 = new Combobox();
		hbox.appendChild(subMatapelajaran3);

		subMatapelajaran3.setCols(12);
		subMatapelajaran3.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 3);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari4.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari4.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari4() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran IV"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari4 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari4.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari4.appendChild(comboitem);

		Common.selectComboItem(true, hari4, guruMengajar.getHari4());

		hbox.appendChild(hari4);

		hari4.setCols(5);
		hari4.setReadonly(true);

		jamPelajaran4 = new Combobox();
		hbox.appendChild(jamPelajaran4);

		jamPelajaran4.setCols(12);
		jamPelajaran4.setReadonly(true);

		subMatapelajaran4 = new Combobox();
		hbox.appendChild(subMatapelajaran4);

		subMatapelajaran4.setCols(12);
		subMatapelajaran4.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 4);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari5.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari5.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari5() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran V"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari5 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari5.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari5.appendChild(comboitem);

		Common.selectComboItem(true, hari5, guruMengajar.getHari5());
		hbox.appendChild(hari5);
		hari5.setCols(5);
		hari5.setReadonly(true);

		jamPelajaran5 = new Combobox();
		hbox.appendChild(jamPelajaran5);
		jamPelajaran5.setCols(12);
		jamPelajaran5.setReadonly(true);

		subMatapelajaran5 = new Combobox();
		hbox.appendChild(subMatapelajaran5);

		subMatapelajaran5.setCols(12);
		subMatapelajaran5.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 5);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari6.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari6.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari6() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran VI"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari6 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari6.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari6.appendChild(comboitem);

		Common.selectComboItem(true, hari6, guruMengajar.getHari6());
		hbox.appendChild(hari6);
		hari6.setCols(6);
		hari6.setReadonly(true);

		jamPelajaran6 = new Combobox();
		hbox.appendChild(jamPelajaran6);
		jamPelajaran6.setCols(12);
		jamPelajaran6.setReadonly(true);

		subMatapelajaran6 = new Combobox();
		hbox.appendChild(subMatapelajaran6);

		subMatapelajaran6.setCols(12);
		subMatapelajaran6.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 6);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari7.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari7.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari7() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran VII"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari7 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari7.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari7.appendChild(comboitem);

		Common.selectComboItem(true, hari7, guruMengajar.getHari7());

		hbox.appendChild(hari7);
		hari7.setCols(7);
		hari7.setReadonly(true);

		jamPelajaran7 = new Combobox();
		hbox.appendChild(jamPelajaran7);

		jamPelajaran7.setCols(12);
		jamPelajaran7.setReadonly(true);

		subMatapelajaran7 = new Combobox();
		hbox.appendChild(subMatapelajaran7);

		subMatapelajaran7.setCols(12);
		subMatapelajaran7.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 7);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari8.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari8.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari8() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran VIII"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari8 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari8.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari8.appendChild(comboitem);

		Common.selectComboItem(true, hari8, guruMengajar.getHari8());

		hbox.appendChild(hari8);

		hari8.setCols(12);
		hari8.setReadonly(true);

		jamPelajaran8 = new Combobox();
		hbox.appendChild(jamPelajaran8);

		jamPelajaran8.setCols(12);
		jamPelajaran8.setReadonly(true);

		subMatapelajaran8 = new Combobox();
		hbox.appendChild(subMatapelajaran8);

		subMatapelajaran8.setCols(12);
		subMatapelajaran8.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 8);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari9.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari9.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari9() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran IX"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari9 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari9.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari9.appendChild(comboitem);

		Common.selectComboItem(true, hari9, guruMengajar.getHari9());

		hbox.appendChild(hari9);

		hari9.setCols(9);
		hari9.setReadonly(true);

		jamPelajaran9 = new Combobox();
		hbox.appendChild(jamPelajaran9);
		jamPelajaran9.setCols(12);
		jamPelajaran9.setReadonly(true);

		subMatapelajaran9 = new Combobox();
		hbox.appendChild(subMatapelajaran9);

		subMatapelajaran9.setCols(12);
		subMatapelajaran9.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 9);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari10.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari10.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari10() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran X"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari10 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari10.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari10.appendChild(comboitem);

		Common.selectComboItem(true, hari10, guruMengajar.getHari10());

		hbox.appendChild(hari10);

		hari10.setCols(10);
		hari10.setReadonly(true);

		jamPelajaran10 = new Combobox();
		hbox.appendChild(jamPelajaran10);

		jamPelajaran10.setCols(12);
		jamPelajaran10.setReadonly(true);

		subMatapelajaran10 = new Combobox();
		hbox.appendChild(subMatapelajaran10);

		subMatapelajaran10.setCols(12);
		subMatapelajaran10.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 10);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari11.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
				hari11.getParent().getParent().getNextSibling().setVisible(true);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari11() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XI"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari11 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari11.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari11.appendChild(comboitem);

		Common.selectComboItem(true, hari11, guruMengajar.getHari11());

		hbox.appendChild(hari11);

		hari11.setCols(11);
		hari11.setReadonly(true);

		jamPelajaran11 = new Combobox();
		hbox.appendChild(jamPelajaran11);

		jamPelajaran11.setCols(12);
		jamPelajaran11.setReadonly(true);

		subMatapelajaran11 = new Combobox();
		hbox.appendChild(subMatapelajaran11);

		subMatapelajaran11.setCols(12);
		subMatapelajaran11.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 11);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari12.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari12() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XII"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari12 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari12.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari12.appendChild(comboitem);

		Common.selectComboItem(true, hari12, guruMengajar.getHari12());

		hbox.appendChild(hari12);

		hari12.setCols(12);
		hari12.setReadonly(true);

		jamPelajaran12 = new Combobox();
		hbox.appendChild(jamPelajaran12);

		jamPelajaran12.setCols(12);
		jamPelajaran12.setReadonly(true);

		subMatapelajaran12 = new Combobox();
		hbox.appendChild(subMatapelajaran12);

		subMatapelajaran12.setCols(12);
		subMatapelajaran12.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 12);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari13.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari13() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XIII"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari13 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari13.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari13.appendChild(comboitem);

		Common.selectComboItem(true, hari13, guruMengajar.getHari13());

		hbox.appendChild(hari13);

		hari13.setCols(12);
		hari13.setReadonly(true);

		jamPelajaran13 = new Combobox();
		hbox.appendChild(jamPelajaran13);

		jamPelajaran13.setCols(12);
		jamPelajaran13.setReadonly(true);

		subMatapelajaran13 = new Combobox();
		hbox.appendChild(subMatapelajaran13);

		subMatapelajaran13.setCols(12);
		subMatapelajaran13.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 13);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari14.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari14() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XIV"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari14 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari14.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari14.appendChild(comboitem);

		Common.selectComboItem(true, hari14, guruMengajar.getHari14());

		hbox.appendChild(hari14);

		hari14.setCols(12);
		hari14.setReadonly(true);

		jamPelajaran14 = new Combobox();
		hbox.appendChild(jamPelajaran14);

		jamPelajaran14.setCols(12);
		jamPelajaran14.setReadonly(true);

		subMatapelajaran14 = new Combobox();
		hbox.appendChild(subMatapelajaran14);

		subMatapelajaran14.setCols(12);
		subMatapelajaran14.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 14);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari15.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari15() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XV"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari15 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari15.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari15.appendChild(comboitem);

		Common.selectComboItem(true, hari15, guruMengajar.getHari15());

		hbox.appendChild(hari15);

		hari15.setCols(12);
		hari15.setReadonly(true);

		jamPelajaran15 = new Combobox();
		hbox.appendChild(jamPelajaran15);

		jamPelajaran15.setCols(12);
		jamPelajaran15.setReadonly(true);

		subMatapelajaran15 = new Combobox();
		hbox.appendChild(subMatapelajaran15);

		subMatapelajaran15.setCols(12);
		subMatapelajaran15.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 15);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari16.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari16() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XVI"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari16 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari16.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari16.appendChild(comboitem);

		Common.selectComboItem(true, hari16, guruMengajar.getHari16());

		hbox.appendChild(hari16);

		hari16.setCols(12);
		hari16.setReadonly(true);

		jamPelajaran16 = new Combobox();
		hbox.appendChild(jamPelajaran16);

		jamPelajaran16.setCols(12);
		jamPelajaran16.setReadonly(true);

		subMatapelajaran16 = new Combobox();
		hbox.appendChild(subMatapelajaran16);

		subMatapelajaran16.setCols(12);
		subMatapelajaran16.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 16);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari17.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari17() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XVII"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari17 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari17.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari17.appendChild(comboitem);

		Common.selectComboItem(true, hari17, guruMengajar.getHari17());

		hbox.appendChild(hari17);

		hari17.setCols(12);
		hari17.setReadonly(true);

		jamPelajaran17 = new Combobox();
		hbox.appendChild(jamPelajaran17);

		jamPelajaran17.setCols(12);
		jamPelajaran17.setReadonly(true);

		subMatapelajaran17 = new Combobox();
		hbox.appendChild(subMatapelajaran17);

		subMatapelajaran17.setCols(12);
		subMatapelajaran17.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 17);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari18.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari18() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XVIII"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari18 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari18.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari18.appendChild(comboitem);

		Common.selectComboItem(true, hari18, guruMengajar.getHari18());

		hbox.appendChild(hari18);

		hari18.setCols(12);
		hari18.setReadonly(true);

		jamPelajaran18 = new Combobox();
		hbox.appendChild(jamPelajaran18);

		jamPelajaran18.setCols(12);
		jamPelajaran18.setReadonly(true);

		subMatapelajaran18 = new Combobox();
		hbox.appendChild(subMatapelajaran18);

		subMatapelajaran18.setCols(12);
		subMatapelajaran18.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 18);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari19.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari19() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XVIX"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari19 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari19.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari19.appendChild(comboitem);

		Common.selectComboItem(true, hari19, guruMengajar.getHari19());

		hbox.appendChild(hari19);

		hari19.setCols(12);
		hari19.setReadonly(true);

		jamPelajaran19 = new Combobox();
		hbox.appendChild(jamPelajaran19);

		jamPelajaran19.setCols(12);
		jamPelajaran19.setReadonly(true);

		subMatapelajaran19 = new Combobox();
		hbox.appendChild(subMatapelajaran19);

		subMatapelajaran19.setCols(12);
		subMatapelajaran19.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 19);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari20.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari20() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XX"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari20 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari20.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari20.appendChild(comboitem);

		Common.selectComboItem(true, hari20, guruMengajar.getHari20());

		hbox.appendChild(hari20);

		hari20.setCols(12);
		hari20.setReadonly(true);

		jamPelajaran20 = new Combobox();
		hbox.appendChild(jamPelajaran20);

		jamPelajaran20.setCols(12);
		jamPelajaran20.setReadonly(true);

		subMatapelajaran20 = new Combobox();
		hbox.appendChild(subMatapelajaran20);

		subMatapelajaran20.setCols(12);
		subMatapelajaran20.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 20);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari21.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari21() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XXI"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari21 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari21.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari21.appendChild(comboitem);

		Common.selectComboItem(true, hari21, guruMengajar.getHari21());

		hbox.appendChild(hari21);

		hari21.setCols(12);
		hari21.setReadonly(true);

		jamPelajaran21 = new Combobox();
		hbox.appendChild(jamPelajaran21);

		jamPelajaran21.setCols(12);
		jamPelajaran21.setReadonly(true);

		subMatapelajaran21 = new Combobox();
		hbox.appendChild(subMatapelajaran21);

		subMatapelajaran21.setCols(12);
		subMatapelajaran21.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 21);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari22.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari22() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XXII"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari22 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari22.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari22.appendChild(comboitem);

		Common.selectComboItem(true, hari22, guruMengajar.getHari22());

		hbox.appendChild(hari22);

		hari22.setCols(12);
		hari22.setReadonly(true);

		jamPelajaran22 = new Combobox();
		hbox.appendChild(jamPelajaran22);

		jamPelajaran22.setCols(12);
		jamPelajaran22.setReadonly(true);

		subMatapelajaran22 = new Combobox();
		hbox.appendChild(subMatapelajaran22);

		subMatapelajaran22.setCols(12);
		subMatapelajaran22.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 22);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari23.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari23() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XXIII"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari23 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari23.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari23.appendChild(comboitem);

		Common.selectComboItem(true, hari23, guruMengajar.getHari23());

		hbox.appendChild(hari23);

		hari23.setCols(12);
		hari23.setReadonly(true);

		jamPelajaran23 = new Combobox();
		hbox.appendChild(jamPelajaran23);

		jamPelajaran23.setCols(12);
		jamPelajaran23.setReadonly(true);

		subMatapelajaran23 = new Combobox();
		hbox.appendChild(subMatapelajaran23);

		subMatapelajaran23.setCols(12);
		subMatapelajaran23.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 23);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari24.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari24() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XXIX"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari24 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari24.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari24.appendChild(comboitem);

		Common.selectComboItem(true, hari24, guruMengajar.getHari24());

		hbox.appendChild(hari24);

		hari24.setCols(12);
		hari24.setReadonly(true);

		jamPelajaran24 = new Combobox();
		hbox.appendChild(jamPelajaran24);

		jamPelajaran24.setCols(12);
		jamPelajaran24.setReadonly(true);

		subMatapelajaran24 = new Combobox();
		hbox.appendChild(subMatapelajaran24);

		subMatapelajaran24.setCols(12);
		subMatapelajaran24.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(hrs.size() == 24);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig("Tambah Jadwal Mengajar", "/img/svg/addthis.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hari25.getParent().getParent().setVisible(true);
				arg0.getTarget().getParent().setVisible(false);
			}
		});

		row = new MyFormRow();
		row.setVisible(guruMengajar.getHari25() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari dan Jam Pelajaran XXV"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hari25 = new Combobox();
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari25.appendChild(comboitem);

		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak ada jadwal");
		comboitem.setValue(null);
		hari25.appendChild(comboitem);

		Common.selectComboItem(true, hari25, guruMengajar.getHari25());

		hbox.appendChild(hari25);

		hari25.setCols(12);
		hari25.setReadonly(true);

		jamPelajaran25 = new Combobox();
		hbox.appendChild(jamPelajaran25);

		jamPelajaran25.setCols(12);
		jamPelajaran25.setReadonly(true);

		subMatapelajaran25 = new Combobox();
		hbox.appendChild(subMatapelajaran25);

		subMatapelajaran25.setCols(12);
		subMatapelajaran25.setReadonly(true);

		hari.setCols(6);
		hari2.setCols(6);
		hari3.setCols(6);
		hari4.setCols(6);
		hari5.setCols(6);
		hari6.setCols(6);
		hari7.setCols(6);
		hari8.setCols(6);
		hari9.setCols(6);
		hari10.setCols(6);
		hari11.setCols(6);
		hari12.setCols(6);
		hari13.setCols(6);
		hari14.setCols(6);
		hari15.setCols(6);
		hari16.setCols(6);
		hari17.setCols(6);
		hari18.setCols(6);
		hari19.setCols(6);
		hari20.setCols(6);
		hari21.setCols(6);
		hari22.setCols(6);
		hari23.setCols(6);
		hari24.setCols(6);
		hari25.setCols(6);

		final EventListener eventListenerSekolah = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);

				Matapelajaran mk = (Matapelajaran) (matapelajaran.getSelectedItem() == null ? null
						: matapelajaran.getSelectedItem().getValue());

				List<SubMatapelajaran> subMatapelajarans = ConstantValues.simpleList(HibernateUtil.currentSession()
						.createCriteria(SubMatapelajaran.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("matapelajaran", mk)).addOrder(Order.asc("nama")), SubMatapelajaran.class);

				if (subMatapelajarans.isEmpty()) {
					subMatapelajaran.setVisible(false);
					subMatapelajaran2.setVisible(false);
					subMatapelajaran3.setVisible(false);
					subMatapelajaran4.setVisible(false);
					subMatapelajaran5.setVisible(false);
					subMatapelajaran6.setVisible(false);
					subMatapelajaran7.setVisible(false);
					subMatapelajaran8.setVisible(false);
					subMatapelajaran9.setVisible(false);
					subMatapelajaran10.setVisible(false);
					subMatapelajaran11.setVisible(false);
					subMatapelajaran12.setVisible(false);
					subMatapelajaran13.setVisible(false);

					subMatapelajaran14.setVisible(false);
					subMatapelajaran15.setVisible(false);
					subMatapelajaran16.setVisible(false);
					subMatapelajaran17.setVisible(false);
					subMatapelajaran18.setVisible(false);
					subMatapelajaran19.setVisible(false);
					subMatapelajaran20.setVisible(false);
					subMatapelajaran21.setVisible(false);
					subMatapelajaran22.setVisible(false);
					subMatapelajaran23.setVisible(false);
					subMatapelajaran24.setVisible(false);
					subMatapelajaran25.setVisible(false);
				} else {
					Common.insertComboItems(subMatapelajaran, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran2, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran3, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran4, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran5, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran6, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran7, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran8, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran9, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran10, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran11, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran12, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran13, "nama", subMatapelajarans);

					Common.insertComboItems(subMatapelajaran14, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran15, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran16, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran17, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran18, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran19, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran20, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran21, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran22, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran23, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran24, "nama", subMatapelajarans);
					Common.insertComboItems(subMatapelajaran25, "nama", subMatapelajarans);
				}

				Common.insertCombo(jamPelajaran, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				MyComboitemConfig comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran, guruMengajar.getJamPelajaran());

				Common.insertCombo(jamPelajaran2, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran2.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran2, guruMengajar.getJamPelajaran2());

				Common.insertCombo(jamPelajaran3, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran3.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran3, guruMengajar.getJamPelajaran3());

				Common.insertCombo(jamPelajaran4, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran4.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran4, guruMengajar.getJamPelajaran4());

				Common.insertCombo(jamPelajaran5, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran5.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran5, guruMengajar.getJamPelajaran5());

				Common.insertCombo(jamPelajaran6, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran6.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran6, guruMengajar.getJamPelajaran6());

				Common.insertCombo(jamPelajaran7, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran7.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran7, guruMengajar.getJamPelajaran7());

				Common.insertCombo(jamPelajaran8, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran8.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran8, guruMengajar.getJamPelajaran8());

				Common.insertCombo(jamPelajaran9, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran9.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran9, guruMengajar.getJamPelajaran9());

				Common.insertCombo(jamPelajaran10, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran10.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran10, guruMengajar.getJamPelajaran10());

				Common.insertCombo(jamPelajaran11, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran11.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran11, guruMengajar.getJamPelajaran11());

				Common.insertCombo(jamPelajaran12, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran12.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran12, guruMengajar.getJamPelajaran12());

				Common.insertCombo(jamPelajaran13, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran13.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran13, guruMengajar.getJamPelajaran13());

				Common.insertCombo(jamPelajaran14, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran14.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran14, guruMengajar.getJamPelajaran14());

				Common.insertCombo(jamPelajaran15, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran15.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran15, guruMengajar.getJamPelajaran15());

				Common.insertCombo(jamPelajaran16, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran16.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran16, guruMengajar.getJamPelajaran16());

				Common.insertCombo(jamPelajaran17, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran17.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran17, guruMengajar.getJamPelajaran17());

				Common.insertCombo(jamPelajaran18, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran18.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran18, guruMengajar.getJamPelajaran18());

				Common.insertCombo(jamPelajaran19, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran19.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran19, guruMengajar.getJamPelajaran19());

				Common.insertCombo(jamPelajaran20, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran20.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran20, guruMengajar.getJamPelajaran20());

				Common.insertCombo(jamPelajaran21, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran21.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran21, guruMengajar.getJamPelajaran21());

				Common.insertCombo(jamPelajaran22, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran22.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran22, guruMengajar.getJamPelajaran22());

				Common.insertCombo(jamPelajaran23, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran23.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran23, guruMengajar.getJamPelajaran23());

				Common.insertCombo(jamPelajaran24, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran24.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran24, guruMengajar.getJamPelajaran24());

				Common.insertCombo(jamPelajaran25, new String[] { "nama", "mulaiS", "sampaiS", "jenisJadwalPelajaran" },
						JamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				comboitem = new MyComboitemConfig();
				comboitem.setLabel("Tidak ada jadwal");
				comboitem.setValue(null);
				jamPelajaran25.appendChild(comboitem);

				Common.selectComboItem(true, jamPelajaran25, guruMengajar.getJamPelajaran25());

			}
		};

		matapelajaran.addEventListener("onChange", eventListenerSekolah);

		EventListener kelasListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);

				Common.insertCombo(matapelajaran, new String[] { "nama", "jenisPenilaian", "sekolah" },
						Matapelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(true, matapelajaran, guruMengajar.getMatapelajaran());
				matapelajaran.setReadonly(true);

				eventListenerSekolah.onEvent(arg0);

			}
		};

		sekolah.addEventListener("onChange", kelasListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(guruMengajar.getKeterangan()));
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

					if (eventListener != null) {
						eventListener.onEvent(new Event("", event.getTarget(), GuruMengajarAction.this.guruMengajar));
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		Common.createDefaultTimer(eventListenerSekolah);
		Common.createDefaultTimer(kelasListener);

	}

	public boolean onSave(Event event) throws Exception {

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
		if (guru.getAttribute("guru") == null) {
			MyMessageboxConfig.show("Guru harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (matapelajaran.getSelectedItem() == null || matapelajaran.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Matapelajaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Matapelajaran mt = (Matapelajaran) matapelajaran.getSelectedItem().getValue();
		Guru g = (Guru) guru.getAttribute("guru");
		Session session = HibernateUtil.currentSession();
		if (guruMengajar.getId() != null) {
			guruMengajar = (GuruMengajar) session.load(GuruMengajar.class, guruMengajar.getId());
		} else {
			GuruMengajar a = (GuruMengajar) ConstantValues.simpleObject(session.createCriteria(GuruMengajar.class)
					.add(Restrictions.eq("guru", g)).add(Restrictions.eq("matapelajaran", mt)).setMaxResults(1),
					GuruMengajar.class);
			if (a != null) {

				MyMessageboxConfig.showFormat(
						"Mohon maaf, data Guru \"{V1}\" yang mengajar matapelajaran \"{V2}\" sudah tersedia sebelumnya, sehingga tidak dapat ditambahkan kembali. Langkah yang dapat dilakukan: (1) Periksa kembali daftar guru mengajar yang telah ada; (2) Pilih kombinasi guru dan matapelajaran yang berbeda; (3) Ubah data yang sudah ada apabila diperlukan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, guru, mt.getNama());

				return false;
			}
		}
		guruMengajar.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));
		guruMengajar.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		guruMengajar.setMatapelajaran(mt);

		guruMengajar.setKeterangan(keterangan.getValue());

		guruMengajar.setJamPelajaran((JamPelajaran) (jamPelajaran.getSelectedItem() == null ? null
				: jamPelajaran.getSelectedItem().getValue()));
		guruMengajar.setHari((String) (hari.getSelectedItem() == null ? null : hari.getSelectedItem().getValue()));

		guruMengajar.setJamPelajaran2((JamPelajaran) (jamPelajaran2.getSelectedItem() == null ? null
				: jamPelajaran2.getSelectedItem().getValue()));
		guruMengajar.setHari2((String) (hari2.getSelectedItem() == null ? null : hari2.getSelectedItem().getValue()));

		guruMengajar.setJamPelajaran3((JamPelajaran) (jamPelajaran3.getSelectedItem() == null ? null
				: jamPelajaran3.getSelectedItem().getValue()));
		guruMengajar.setHari3((String) (hari3.getSelectedItem() == null ? null : hari3.getSelectedItem().getValue()));

		guruMengajar.setJamPelajaran4((JamPelajaran) (jamPelajaran4.getSelectedItem() == null ? null
				: jamPelajaran4.getSelectedItem().getValue()));
		guruMengajar.setHari4((String) (hari4.getSelectedItem() == null ? null : hari4.getSelectedItem().getValue()));

		guruMengajar.setJamPelajaran5((JamPelajaran) (jamPelajaran5.getSelectedItem() == null ? null
				: jamPelajaran5.getSelectedItem().getValue()));
		guruMengajar.setHari5((String) (hari5.getSelectedItem() == null ? null : hari5.getSelectedItem().getValue()));

		guruMengajar.setJamPelajaran6((JamPelajaran) (jamPelajaran6.getSelectedItem() == null ? null
				: jamPelajaran6.getSelectedItem().getValue()));
		guruMengajar.setHari6((String) (hari6.getSelectedItem() == null ? null : hari6.getSelectedItem().getValue()));

		guruMengajar.setJamPelajaran7((JamPelajaran) (jamPelajaran7.getSelectedItem() == null ? null
				: jamPelajaran7.getSelectedItem().getValue()));
		guruMengajar.setHari7((String) (hari7.getSelectedItem() == null ? null : hari7.getSelectedItem().getValue()));

		guruMengajar.setJamPelajaran8((JamPelajaran) (jamPelajaran8.getSelectedItem() == null ? null
				: jamPelajaran8.getSelectedItem().getValue()));
		guruMengajar.setHari8((String) (hari8.getSelectedItem() == null ? null : hari8.getSelectedItem().getValue()));

		guruMengajar.setJamPelajaran9((JamPelajaran) (jamPelajaran9.getSelectedItem() == null ? null
				: jamPelajaran9.getSelectedItem().getValue()));
		guruMengajar.setHari9((String) (hari9.getSelectedItem() == null ? null : hari9.getSelectedItem().getValue()));

		guruMengajar.setJamPelajaran10((JamPelajaran) (jamPelajaran10.getSelectedItem() == null ? null
				: jamPelajaran10.getSelectedItem().getValue()));
		guruMengajar
				.setHari10((String) (hari10.getSelectedItem() == null ? null : hari10.getSelectedItem().getValue()));

		guruMengajar.setJamPelajaran11((JamPelajaran) (jamPelajaran11.getSelectedItem() == null ? null
				: jamPelajaran11.getSelectedItem().getValue()));
		guruMengajar
				.setHari11((String) (hari11.getSelectedItem() == null ? null : hari11.getSelectedItem().getValue()));

		guruMengajar.setJamPelajaran12((JamPelajaran) (jamPelajaran12.getSelectedItem() == null ? null
				: jamPelajaran12.getSelectedItem().getValue()));
		guruMengajar
				.setHari12((String) (hari12.getSelectedItem() == null ? null : hari12.getSelectedItem().getValue()));

		guruMengajar.setGuru((Guru) guru.getAttribute("guru"));

		if (tbmuser != null && tbmuser.ambilGuru() != null) {
			if (guruMengajar.getGuru() == null) {
				guruMengajar.setGuru(tbmuser.ambilGuru());
			}
		}

		guruMengajar.setSubMatapelajaran((SubMatapelajaran) (subMatapelajaran.getSelectedItem() == null ? null
				: subMatapelajaran.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran2((SubMatapelajaran) (subMatapelajaran2.getSelectedItem() == null ? null
				: subMatapelajaran2.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran3((SubMatapelajaran) (subMatapelajaran3.getSelectedItem() == null ? null
				: subMatapelajaran3.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran4((SubMatapelajaran) (subMatapelajaran4.getSelectedItem() == null ? null
				: subMatapelajaran4.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran5((SubMatapelajaran) (subMatapelajaran5.getSelectedItem() == null ? null
				: subMatapelajaran5.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran6((SubMatapelajaran) (subMatapelajaran6.getSelectedItem() == null ? null
				: subMatapelajaran6.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran7((SubMatapelajaran) (subMatapelajaran7.getSelectedItem() == null ? null
				: subMatapelajaran7.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran8((SubMatapelajaran) (subMatapelajaran8.getSelectedItem() == null ? null
				: subMatapelajaran8.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran9((SubMatapelajaran) (subMatapelajaran9.getSelectedItem() == null ? null
				: subMatapelajaran9.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran10((SubMatapelajaran) (subMatapelajaran10.getSelectedItem() == null ? null
				: subMatapelajaran10.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran11((SubMatapelajaran) (subMatapelajaran11.getSelectedItem() == null ? null
				: subMatapelajaran11.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran12((SubMatapelajaran) (subMatapelajaran12.getSelectedItem() == null ? null
				: subMatapelajaran12.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran13((SubMatapelajaran) (subMatapelajaran13.getSelectedItem() == null ? null
				: subMatapelajaran13.getSelectedItem().getValue()));
		guruMengajar
				.setHari13((String) (hari13.getSelectedItem() == null ? null : hari13.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran13((JamPelajaran) (jamPelajaran13.getSelectedItem() == null ? null
				: jamPelajaran13.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran14((SubMatapelajaran) (subMatapelajaran14.getSelectedItem() == null ? null
				: subMatapelajaran14.getSelectedItem().getValue()));
		guruMengajar
				.setHari14((String) (hari14.getSelectedItem() == null ? null : hari14.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran14((JamPelajaran) (jamPelajaran14.getSelectedItem() == null ? null
				: jamPelajaran14.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran15((SubMatapelajaran) (subMatapelajaran15.getSelectedItem() == null ? null
				: subMatapelajaran15.getSelectedItem().getValue()));
		guruMengajar
				.setHari15((String) (hari15.getSelectedItem() == null ? null : hari15.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran15((JamPelajaran) (jamPelajaran15.getSelectedItem() == null ? null
				: jamPelajaran15.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran16((SubMatapelajaran) (subMatapelajaran16.getSelectedItem() == null ? null
				: subMatapelajaran16.getSelectedItem().getValue()));
		guruMengajar
				.setHari16((String) (hari16.getSelectedItem() == null ? null : hari16.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran16((JamPelajaran) (jamPelajaran16.getSelectedItem() == null ? null
				: jamPelajaran16.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran17((SubMatapelajaran) (subMatapelajaran17.getSelectedItem() == null ? null
				: subMatapelajaran17.getSelectedItem().getValue()));
		guruMengajar
				.setHari17((String) (hari17.getSelectedItem() == null ? null : hari17.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran17((JamPelajaran) (jamPelajaran17.getSelectedItem() == null ? null
				: jamPelajaran17.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran18((SubMatapelajaran) (subMatapelajaran18.getSelectedItem() == null ? null
				: subMatapelajaran18.getSelectedItem().getValue()));
		guruMengajar
				.setHari18((String) (hari18.getSelectedItem() == null ? null : hari18.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran18((JamPelajaran) (jamPelajaran18.getSelectedItem() == null ? null
				: jamPelajaran18.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran19((SubMatapelajaran) (subMatapelajaran19.getSelectedItem() == null ? null
				: subMatapelajaran19.getSelectedItem().getValue()));
		guruMengajar
				.setHari19((String) (hari19.getSelectedItem() == null ? null : hari19.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran19((JamPelajaran) (jamPelajaran19.getSelectedItem() == null ? null
				: jamPelajaran19.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran20((SubMatapelajaran) (subMatapelajaran20.getSelectedItem() == null ? null
				: subMatapelajaran20.getSelectedItem().getValue()));
		guruMengajar
				.setHari20((String) (hari20.getSelectedItem() == null ? null : hari20.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran20((JamPelajaran) (jamPelajaran20.getSelectedItem() == null ? null
				: jamPelajaran20.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran21((SubMatapelajaran) (subMatapelajaran21.getSelectedItem() == null ? null
				: subMatapelajaran21.getSelectedItem().getValue()));
		guruMengajar
				.setHari21((String) (hari21.getSelectedItem() == null ? null : hari21.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran21((JamPelajaran) (jamPelajaran21.getSelectedItem() == null ? null
				: jamPelajaran21.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran22((SubMatapelajaran) (subMatapelajaran22.getSelectedItem() == null ? null
				: subMatapelajaran22.getSelectedItem().getValue()));
		guruMengajar
				.setHari22((String) (hari22.getSelectedItem() == null ? null : hari22.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran22((JamPelajaran) (jamPelajaran22.getSelectedItem() == null ? null
				: jamPelajaran22.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran23((SubMatapelajaran) (subMatapelajaran23.getSelectedItem() == null ? null
				: subMatapelajaran23.getSelectedItem().getValue()));
		guruMengajar
				.setHari23((String) (hari23.getSelectedItem() == null ? null : hari23.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran23((JamPelajaran) (jamPelajaran23.getSelectedItem() == null ? null
				: jamPelajaran23.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran24((SubMatapelajaran) (subMatapelajaran24.getSelectedItem() == null ? null
				: subMatapelajaran24.getSelectedItem().getValue()));
		guruMengajar
				.setHari24((String) (hari24.getSelectedItem() == null ? null : hari24.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran24((JamPelajaran) (jamPelajaran24.getSelectedItem() == null ? null
				: jamPelajaran24.getSelectedItem().getValue()));

		guruMengajar.setSubMatapelajaran25((SubMatapelajaran) (subMatapelajaran25.getSelectedItem() == null ? null
				: subMatapelajaran25.getSelectedItem().getValue()));
		guruMengajar
				.setHari25((String) (hari25.getSelectedItem() == null ? null : hari25.getSelectedItem().getValue()));
		guruMengajar.setJamPelajaran25((JamPelajaran) (jamPelajaran25.getSelectedItem() == null ? null
				: jamPelajaran25.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, guruMengajar);
		session.flush();

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GuruMengajar.class)

				.createAlias("matapelajaran", "matapelajaran");

		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria

				.add(searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						:

						Restrictions.or(
								Restrictions
										.or(Restrictions.eq("hari", searchhari.getSelectedItem().getValue()),
												Restrictions
														.or(Restrictions.eq("hari5",
																searchhari.getSelectedItem().getValue()),
																Restrictions.or(
																		Restrictions.eq("hari4",
																				searchhari.getSelectedItem()
																						.getValue()),
																		Restrictions.or(
																				Restrictions.eq("hari3",
																						searchhari.getSelectedItem()
																								.getValue()),
																				Restrictions.eq("hari2",
																						searchhari.getSelectedItem()
																								.getValue()))))),

								Restrictions.or(Restrictions.eq("hari6", searchhari.getSelectedItem().getValue()),
										Restrictions.or(
												Restrictions.eq("hari7", searchhari.getSelectedItem().getValue()),
												Restrictions.or(
														Restrictions.eq("hari8",
																searchhari.getSelectedItem().getValue()),
														Restrictions.or(
																Restrictions.eq("hari9",
																		searchhari.getSelectedItem().getValue()),
																Restrictions.eq("hari10",
																		searchhari.getSelectedItem().getValue()))))))

				)

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("matapelajaran.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchguru == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchguru.getAttribute("guru") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("guru", searchguru.getAttribute("guru"))))

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

		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<GuruMengajar> guruMengajar = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(guruMengajar);
		grid.setRowRenderer(new GuruMengajarRenderer());
		grid.setModelCheckMobile(strset);

	}

}
