package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import ais.action.master.prestasi.DasbordPrestasi;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardRekapPrestasiDosenBerdasarCabang;
import ais.action.master.dashboard.admin.DashboardRekapPrestasiDosenBerdasarKategori;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.akademik.LaporanPrestasiDosen;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.Html2Text;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CabangPrestasiDosen;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.KategoriPrestasiDosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.PrestasiDosen;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PrestasiDosenAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchpenyelenggara;
	private AmbilDataDosenBanbox searchdosen;
	private MyDatebox searchmulai;
	private Combobox searchta;
	private MyDatebox searchsampai;
	private Combobox searchstatus;
	private Combobox searchjurusan;
	private Combobox searchfakultas;
	private Combobox searchcabangPrestasiDosen;
	private Combobox searchkategoriPrestasiDosen;

	private Textbox nama;
	private MyDatebox tanggal;
	private MyDatebox tanggalSelesai;
	private AmbilDataDosenBanbox dosen;
	private Combobox jurusan;
	private Combobox fakultas;
	private Checkbox prestasiLuarKampus;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Textbox keterangan;

	private PrestasiDosen prestasiDosen;
	private MyToolbarbuttonConfig add;

	protected LampiranLain lainDosen;
	private Tbmuser tbmuser;
	private Textbox tempat;
	private Textbox juara;
	private Intbox peringkat;
	private Textbox penyelenggara;
	private Textbox nomorSertifikat;

	private MyToolbarbuttonConfig uploadData;

	private Combobox cabangPrestasiDosen;
	private Combobox kategoriPrestasiDosen;
	private Textbox jumlahPeserta;
	private Textbox capaian;
	private Textbox url;

	private Dosen mhs;
	private Row rowFakultas;
	private Row rowJurusan;

	private Tabpanel tabDasbor;

	public void onDasbor(Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordPrestasi dasbord = new DasbordPrestasi(DasbordPrestasi.Lingkup.DOSEN);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Prestasi Dosen",
				"Tren dan distribusi pencapaian dosen dalam penelitian, pengabdian, dan penghargaan.");
		}
	}

	private Tabpanel kategoriPrestasiDosenTab;

	private MyColumnConfig colNama;

	public void onKategoriPrestasiDosen(Event event) {
		if (kategoriPrestasiDosenTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kategoriPrestasiDosenTab);
			MyInclude iframe = new MyInclude("/pages/master/kategori_prestasi_dosen.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel cabangPrestasiDosenTab;

	public void onCabangPrestasiDosen(Event event) {
		if (cabangPrestasiDosenTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(cabangPrestasiDosenTab);
			MyInclude iframe = new MyInclude("/pages/master/cabang_prestasi_dosen.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel cabangRekapTab;

	public void onRekapCabang(Event event) {
		if (cabangRekapTab.getChildren().size() == 0) {
			DashboardRekapPrestasiDosenBerdasarCabang window = new DashboardRekapPrestasiDosenBerdasarCabang();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, cabangRekapTab,
				"Rekap per Cabang", "Sebaran prestasi dosen berdasarkan cabang ilmu atau bidang keahlian.");
		}
	}

	private Tabpanel kategoriRekapTab;
	private Intbox tahun;
	private PrestasiDosen prestasiDosenSelected = null;
	private Textbox namaEn;

	public void onRekapKategori(Event event) {
		if (kategoriRekapTab.getChildren().size() == 0) {
			DashboardRekapPrestasiDosenBerdasarKategori window = new DashboardRekapPrestasiDosenBerdasarKategori();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, kategoriRekapTab,
				"Rekap per Kategori", "Sebaran prestasi dosen berdasarkan kategori kompetisi atau penghargaan.");
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

		tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen") && colNama != null) {
			colNama.setWidth("0px");
		}

		Common.generateTahunAjaranDanSemua(searchta);
		Common.selectComboItem(searchta, null);

		kategoriPrestasiDosenTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.ambilDosen() == null);
		cabangPrestasiDosenTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.ambilDosen() == null);
		cabangRekapTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.ambilDosen() == null);

		kategoriRekapTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.ambilDosen() == null);

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(KategoriPrestasiDosen.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			KategoriPrestasiDosen kategoriPrestasiDosen = new KategoriPrestasiDosen();
			kategoriPrestasiDosen.setNama("Internasional");
			session.save(kategoriPrestasiDosen);

			kategoriPrestasiDosen = new KategoriPrestasiDosen();
			kategoriPrestasiDosen.setNama("Nasional");
			session.save(kategoriPrestasiDosen);

			kategoriPrestasiDosen = new KategoriPrestasiDosen();
			kategoriPrestasiDosen.setNama("Regional");
			session.save(kategoriPrestasiDosen);

			kategoriPrestasiDosen = new KategoriPrestasiDosen();
			kategoriPrestasiDosen.setNama("Lain-Lain");
			session.save(kategoriPrestasiDosen);
		}

		count = ((Number) session.createCriteria(CabangPrestasiDosen.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			CabangPrestasiDosen cabangPrestasiDosen = new CabangPrestasiDosen();
			cabangPrestasiDosen.setNama("Seni");
			session.save(cabangPrestasiDosen);

			cabangPrestasiDosen = new CabangPrestasiDosen();
			cabangPrestasiDosen.setNama("Olah Raga");
			session.save(cabangPrestasiDosen);

			cabangPrestasiDosen = new CabangPrestasiDosen();
			cabangPrestasiDosen.setNama("Kejuaraan Ilmiah");
			session.save(cabangPrestasiDosen);

			cabangPrestasiDosen = new CabangPrestasiDosen();
			cabangPrestasiDosen.setKode("9");
			cabangPrestasiDosen.setNama("Lain-Lain");
			session.save(cabangPrestasiDosen);
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.insertComboDanSemua(searchkategoriPrestasiDosen, "nama", KategoriPrestasiDosen.class);
		Common.insertComboDanSemua(searchcabangPrestasiDosen, "nama", CabangPrestasiDosen.class);

		Comboitem comboitem = new Comboitem(PrestasiDosen.BELUM_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PrestasiDosen.BELUM_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PrestasiDosen.SEDANG_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PrestasiDosen.SEDANG_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PrestasiDosen.DISETUJUI);
		if (comboitem != null) { comboitem.setValue(PrestasiDosen.DISETUJUI); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PrestasiDosen.DITOLAK);
		if (comboitem != null) { comboitem.setValue(PrestasiDosen.DITOLAK); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchstatus.appendChild(comboitem);
		if (searchstatus != null) { searchstatus.setReadonly(true); }
		if (searchstatus != null) { searchstatus.setSelectedItem(comboitem); }

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (execution.getParameter("dosen") != null) {
			mhs = (Dosen) HibernateUtil.currentSession().createCriteria(Dosen.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("dosen")))).uniqueResult();
		} else {
			mhs = tbmuser == null ? null : tbmuser.ambilDosen();
		}

		if (execution.getParameter("jurusan") != null) {
			Jurusan jurusanSelected = (Jurusan) HibernateUtil.currentSession().createCriteria(Jurusan.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("jurusan")))).uniqueResult();
			Common.selectComboItem(true, searchjurusan, jurusanSelected);

			Common.selectComboItem(true, searchfakultas,
					jurusanSelected == null ? null : jurusanSelected.getFakultas());

		}

		if (execution.getParameter("cabangPrestasiDosen") != null) {
			CabangPrestasiDosen cabangPrestasiDosenSelected = (CabangPrestasiDosen) HibernateUtil.currentSession()
					.createCriteria(CabangPrestasiDosen.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("cabangPrestasiDosen"))))
					.uniqueResult();
			Common.selectComboItem(true, searchcabangPrestasiDosen, cabangPrestasiDosenSelected);
		}

		if (execution.getParameter("kategoriPrestasiDosen") != null) {
			KategoriPrestasiDosen kategoriPrestasiDosenSelected = (KategoriPrestasiDosen) HibernateUtil.currentSession()
					.createCriteria(KategoriPrestasiDosen.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("kategoriPrestasiDosen"))))
					.uniqueResult();
			Common.selectComboItem(true, searchkategoriPrestasiDosen, kategoriPrestasiDosenSelected);
		}

		if (execution.getParameter("tahunAjaran") != null) {
			String tahunAjaran = execution.getParameter("tahunAjaran");
			Common.selectComboItem(true, searchta, tahunAjaran);
		}

		if (mhs != null) {
			searchdosen.setAttribute("dosen", mhs);
			searchdosen.setDisabled(true);
			searchdosen.setValue(mhs.getNama());
		}

		if (execution.getParameter("prestasi") != null) {
			prestasiDosenSelected = (PrestasiDosen) GeneralValueObject.ambilData(PrestasiDosen.class,
					execution.getParameter("prestasi").toString());
		}

		onDasbor(null);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "dosen", "nama", "namaEn", "tempat", "penyelenggara", "juara",
				"peringkat", "tanggal", "tanggalSelesai", "nomorSertifikat", "cabangPrestasiDosen",
				"kategoriPrestasiDosen", "jumlahPeserta", "capaian", "url", "fakultas", "jurusan", "tahunAkademik",
				"jenisSemester", "tahun", "status", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PrestasiDosen.class, contents);
		upload.setVisible(
				(add != null && add.isVisible()) && tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.ambilDosen() == null);
		Common.appendKeToolbar(upload, add, comp);

		if (add != null) { add.setVisible(tbmuser != null); }
		if (uploadData != null) { uploadData.setVisible(upload.isVisible()); }

		if (mhs != null) {

		}

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Prestasi Dosen", "/img/print.png");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPrestasiDosen laporan = new LaporanPrestasiDosen();
				laporan.setTitle("Prestasi Dosen");
				laporan.setClosable(true);
				laporan.setHeight("95%");
				laporan.setWidth("90%");
				laporan.setParent(page.getFirstRoot());
				laporan.onModal();
			}
		});
		if (cetak != null) { cetak.setParent(add.getParent()); }

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		Common.appendKeToolbar(exportKeOjs, add, comp);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("prestasi_dosen_terhubung_ke_dspace"));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Intbox intbox = new Intbox(0);
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (intbox.getValue() == 0) {
							MyMessageboxConfig.show(
									"Data tidak ditemukan, khusus untuk prestasi dosen, dosen harus mempunya HOMEBASE PRODI terlebih dahulu sebelum bisa mempublikasikan ke dalam repository",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						onSearchDefault(arg0);
						LogLoginAction.tampilDpsaceLog();
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						try {
							String cookie = DspaceCommon.login();
							List<PrestasiDosen> prestasiDosens = initCriteria(true)
									.add(Restrictions.eq("status", PrestasiDosen.DISETUJUI)).list();
							intbox.setValue(prestasiDosens.size());

							int rowIndex = 1;
							for (PrestasiDosen prestasiDosen : prestasiDosens) {
								label.setValue("Sedang memproses data " + prestasiDosen.toString() + " ("
										+ Common.numberFormat.get().format((rowIndex++) * 100.0 / prestasiDosens.size())
										+ " %)");
								PrestasiDosenAction.getDspace(cookie, prestasiDosen, true);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}
						label.setValue("");
					}
				}).start();
			}
		});

		MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor", "/img/svg/trash.svg");
		Common.appendKeToolbar(batalExport, add, comp);
		batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("prestasi_dosen_terhubung_ke_dspace"));
		batalExport.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(arg0);
											LogLoginAction.tampilDpsaceLog();
										}
									});

									new Thread(new Runnable() {

										@SuppressWarnings("unchecked")
										@Override
										public void run() {
											try {
											try {
												String cookie = DspaceCommon.login();
												List<PrestasiDosen> prestasiDosens = initCriteria(true)
														.createAlias("dosen", "dosen")
														.add(Restrictions.isNotNull("dosen.jurusan"))
														.add(Restrictions.eq("status", PrestasiDosen.DISETUJUI)).list();

												int rowIndex = 1;
												for (PrestasiDosen prestasiDosen : prestasiDosens) {
													label.setValue("Sedang memproses data " + prestasiDosen.toString()
															+ " ("
															+ Common.numberFormat.get().format(
																	(rowIndex++) * 100.0 / prestasiDosens.size())
															+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(PrestasiDosen.class.getName(),
																	prestasiDosen.getId());
													if (dspaceInformation != null) {
														int i = DspaceInformation.delete(cookie,
																"items/" + dspaceInformation.getUuid(),
																dspaceInformation.getPostInfo());
														if (i == 200) {

															Session session = HibernateUtil.currentNativeSession();
															session.getTransaction().begin();
															session.delete(dspaceInformation);
															session.getTransaction().commit();
															HibernateUtil.closeSession();
														}
													}
												}
											} catch (Exception e) {
												// TODO Auto-generated catch
												// block
												Common.tampilErrorJikaAdmin(e);
											}
											label.setValue("");
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
			}
		});
	}

	public static DspaceInformation getDspace(String cookie, PrestasiDosen prestasiDosen, boolean update)
			throws Exception {

		JSONArray jsonArray = new JSONArray();

		String nama = "";
		if (prestasiDosen.getDosen() != null) {
			nama = prestasiDosen.getDosen().getNama();
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

		Html2Text parser = new Html2Text();
		parser.parse(new StringReader(prestasiDosen.getCapaian()));

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", parser.getText());
		jsonArray.put(jsonMetadata);

		if (prestasiDosen.getCabangPrestasiDosen() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.type");
			jsonMetadata.put("value", prestasiDosen.getCabangPrestasiDosen().getNama());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", prestasiDosen.getNama());
		jsonArray.put(jsonMetadata);

		if (prestasiDosen.getKategoriPrestasiDosen() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.subject");
			jsonMetadata.put("value", prestasiDosen.getKategoriPrestasiDosen().getNama());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.publisher");
		jsonMetadata.put("value", prestasiDosen.getPenyelenggara());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.uri");
		jsonMetadata.put("value", prestasiDosen.getUrl());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.issn");
		jsonMetadata.put("value", prestasiDosen.getNomorSertifikat());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.language");
		jsonMetadata.put("value", prestasiDosen.getDosen().getBahasa());
		jsonArray.put(jsonMetadata);

		if (prestasiDosen.getTanggal() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(prestasiDosen.getTanggal()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain lam = LampiranLain.ambil(prestasiDosen.getId(), PrestasiDosen.class.getName());
		if (lam != null) {
			String uri = lam.createLinkUri();
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, prestasiDosen,
				jsonPost.toString(), jsonArray.toString(), update, "items",
				"collections/" + getDspaceTipePrestasiDosen(cookie, prestasiDosen) + "/items", "items/{uuid}/metadata");

		if (lam != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lam, "Sertifikat / Lampiran Bukti Prestasi");
		}

		return dspaceInformation;
	}

	public static DspaceInformation getDspaceTipePrestasiDosen(String cookie, PrestasiDosen prestasiDosen)
			throws Exception {
		Jurusan jurusan = prestasiDosen.getDosen().getJurusan();

		String description = "Prestasi dosen untuk " + Common.getBahasaConfig("Jurusan") + " "
				+ prestasiDosen.getDosen().getJurusan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Prestasi Dosen");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription",
				"Prestasi Dosen " + prestasiDosen.getDosen().getJurusan().getJenjang().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_prestasiDosen_" + jurusan.getId(),
				"");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");

	}

	class PrestasiDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PrestasiDosen prestasiDosen = (PrestasiDosen) arg1;

			try {
				if (prestasiDosenSelected != null && prestasiDosenSelected.getId().equals(prestasiDosen.getId())) {
					arg0.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PrestasiDosenAction.java:676");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);

			Vbox myvbox = new Vbox();
			myvbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(prestasiDosen.getDosen()).setParent(myvbox);

			new Label(prestasiDosen.getDosen().getNidn() + "-" + prestasiDosen.getDosen().getNama()).setParent(myvbox);

			Vbox a = RevisiHelper.createNewRevisi(PrestasiDosen.class, prestasiDosen, prestasiDosen.getNama());
			new Label(prestasiDosen.getNamaEn()).setParent(a);

			a.setParent(arg0);

			myvbox = new Vbox();
			myvbox.setParent(detail);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, prestasiDosen.getId(), PrestasiDosen.class.getName(),
					"Lampiran", false, null, null, false, false, false, false);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new MyLabelAgakKecil("Tempat: " + prestasiDosen.getTempat()).setParent(myvbox);
			new MyLabelAgakKecil("Penyelenggara: " + prestasiDosen.getPenyelenggara()).setParent(myvbox);
			new MyLabelAgakKecil("Juara: " + prestasiDosen.getJuara()).setParent(myvbox);
			new MyLabelAgakKecil(
					"Peringkat: " + (prestasiDosen.getPeringkat() == null ? "" : prestasiDosen.getPeringkat()))
							.setParent(myvbox);
			new MyLabelAgakKecil("Tanggal: "
					+ (prestasiDosen.getTanggal() == null ? "" : Common.dateFormat1.get().format(prestasiDosen.getTanggal()))
					+ (prestasiDosen.getTanggalSelesai() == null ? ""
							: " s.d " + Common.dateFormat1.get().format(prestasiDosen.getTanggalSelesai())))
									.setParent(myvbox);
			new MyLabelAgakKecil("TA/Smt: " + prestasiDosen.getTahunAkademik() + "/" + prestasiDosen.getJenisSemester())
					.setParent(myvbox);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new MyLabelAgakKecil("Cabang: " + (prestasiDosen.getCabangPrestasiDosen() == null ? ""
					: prestasiDosen.getCabangPrestasiDosen().getNama())).setParent(myvbox);
			new MyLabelAgakKecil("Kategori: " + (prestasiDosen.getKategoriPrestasiDosen() == null ? ""
					: prestasiDosen.getKategoriPrestasiDosen().getNama())).setParent(myvbox);
			new MyLabelAgakKecil("Jml Peserta: " + prestasiDosen.getJumlahPeserta()).setParent(myvbox);
			new MyLabelAgakKecil("Link: " + prestasiDosen.getUrl()).setParent(myvbox);

			new Label(prestasiDosen.getCapaian()).setParent(arg0);

			new Label(prestasiDosen.getNomorSertifikat()).setParent(arg0);
			final Hbox toolbar = new Hbox();

			boolean merupakanAtasanLangsung = (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
					&& prestasiDosen.getDosen() != null && prestasiDosen.getDosen().getAtasanlangsung() != null
					&& prestasiDosen.getDosen().getAtasanlangsung().equals(tbmuser.getDosen().getId()));

			System.out.println("merupakanAtasanLangsung => " + merupakanAtasanLangsung);

			if ((mhs == null && tbmuser != null) || merupakanAtasanLangsung) {
				final Combobox status = new Combobox();
				Comboitem comboitem = new Comboitem(PrestasiDosen.BELUM_DIPROSES);
				comboitem.setValue(PrestasiDosen.BELUM_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PrestasiDosen.SEDANG_DIPROSES);
				comboitem.setValue(PrestasiDosen.SEDANG_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PrestasiDosen.DISETUJUI);
				comboitem.setValue(PrestasiDosen.DISETUJUI);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PrestasiDosen.DITOLAK);
				comboitem.setValue(PrestasiDosen.DITOLAK);
				status.appendChild(comboitem);

				Common.selectComboItem(status, prestasiDosen.getStatus());
				status.setParent(arg0);
				status.setReadonly(true);
				status.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						prestasiDosen.setStatus((String) (status.getSelectedItem() == null
								|| status.getSelectedItem().getValue() == null ? null
										: status.getSelectedItem().getValue()));
						Common.refreshUpdate(prestasiDosen);
						toolbar.setVisible(!prestasiDosen.getStatus().equals(PrestasiDosen.DISETUJUI));
					}
				};
				status.addEventListener("onChange", eventListener);
			} else {
				new Label(prestasiDosen.getStatus()).setParent(arg0);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil("Luar kampus ? " + (prestasiDosen.getPrestasiLuarKampus() ? "Ya" : "Tidak"))
					.setParent(vbox);
			if (!prestasiDosen.getPrestasiLuarKampus() && prestasiDosen.getFakultas() != null)
				new MyLabelAgakKecil(
						prestasiDosen.getFakultas() == null ? "Semua" : prestasiDosen.getFakultas().getNama())
								.setParent(vbox);
			if (!prestasiDosen.getPrestasiLuarKampus() && prestasiDosen.getJurusan() != null)
				new MyLabelAgakKecil(
						prestasiDosen.getJurusan() == null ? "Semua" : prestasiDosen.getJurusan().getNama())
								.setParent(vbox);

			new Label(prestasiDosen.getKeterangan()).setParent(arg0);

			toolbar.setVisible(!prestasiDosen.getStatus().equals(PrestasiDosen.DISETUJUI) && tbmuser != null);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(prestasiDosen);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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

											if (prestasiDosenSelected != null
													&& prestasiDosenSelected.getId().equals(prestasiDosen.getId())) {
												prestasiDosenSelected = null;
											}

											Common.refreshDelete(prestasiDosen);
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
		init(new PrestasiDosen());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final PrestasiDosen prestasiDosen) throws Exception {
		this.prestasiDosen = prestasiDosen;
		addWindow.setTitle(prestasiDosen.getId() == null ? "Tambah Prestasi Dosen" : "Ubah Prestasi Dosen");
		Common.clear(addWindow);
		addWindow.setWidth("700px");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kejuaraan *"));
		row.appendChild(nama = new Textbox(prestasiDosen.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kejuaraan (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(prestasiDosen.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kejuaraan *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		tanggal = new MyDatebox(prestasiDosen.getTanggal());
		hbox.appendChild(tanggal);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		tanggalSelesai = new MyDatebox(prestasiDosen.getTanggalSelesai());
		hbox.appendChild(tanggalSelesai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen *"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");
		dosen.setReadonly(true);

		if (mhs != null) {
			dosen.setAttribute("dosen", mhs);
			dosen.setDisabled(true);
			dosen.setValue(mhs.getNama());
		} else {
			dosen.setAttribute("dosen", prestasiDosen.getDosen());
			dosen.setValue(prestasiDosen.getDosen() == null ? "" : prestasiDosen.getDosen().getNama());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat Kejuaraan *"));
		row.appendChild(tempat = new Textbox(prestasiDosen.getTempat()));
		tempat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Juara ke *"));
		row.appendChild(juara = new Textbox(prestasiDosen.getJuara()));
		juara.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peringkat"));
		row.appendChild(peringkat = new Intbox(prestasiDosen.getPeringkat()));
		peringkat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyelenggara *"));
		row.appendChild(penyelenggara = new Textbox(prestasiDosen.getPenyelenggara()));
		penyelenggara.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Sertifikat Prestasi *"));
		row.appendChild(nomorSertifikat = new Textbox(prestasiDosen.getNomorSertifikat()));
		nomorSertifikat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cabang *"));
		row.appendChild(cabangPrestasiDosen = new Combobox());
		Common.insertCombo(cabangPrestasiDosen, "nama", CabangPrestasiDosen.class);
		Common.selectComboItem(cabangPrestasiDosen, prestasiDosen.getCabangPrestasiDosen());
		cabangPrestasiDosen.setWidth("90%");
		cabangPrestasiDosen.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori *"));
		row.appendChild(kategoriPrestasiDosen = new Combobox());
		Common.insertCombo(kategoriPrestasiDosen, "nama", KategoriPrestasiDosen.class);
		Common.selectComboItem(kategoriPrestasiDosen, prestasiDosen.getKategoriPrestasiDosen());
		kategoriPrestasiDosen.setWidth("90%");
		kategoriPrestasiDosen.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Peserta *"));
		row.appendChild(jumlahPeserta = new Textbox(prestasiDosen.getJumlahPeserta()));
		jumlahPeserta.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Capaian *"));
		row.appendChild(capaian = new Textbox(prestasiDosen.getCapaian()));
		capaian.setWidth("90%");
		capaian.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link / URL"));
		row.appendChild(url = new Textbox(prestasiDosen.getUrl()));
		url.setWidth("90%");
		url.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Apakah kejuaraan diluar kampus ?"));
		row.appendChild(prestasiLuarKampus = new Checkbox());
		prestasiLuarKampus.setChecked(prestasiDosen.getPrestasiLuarKampus());

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		if (prestasiDosen.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			prestasiDosen.setFakultas(tbmuser.ambilFakultas());
		}
		rowFakultas = new MyFormRow();
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(fakultas, prestasiDosen.getFakultas());
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		rowJurusan = new MyFormRow();
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, prestasiDosen.getJurusan());

		if (prestasiDosen.getJurusan() == null) {
			if (tbmuser.ambilJurusan() != null
					|| (tbmuser.ambilDosen() != null && tbmuser.ambilDosen().getJurusan() != null)) {
				Common.pilihJurusan(jurusan,
						tbmuser == null || tbmuser.ambilJurusan() == null ? tbmuser.ambilDosen().getJurusan()
								: tbmuser.ambilJurusan());
				jurusan.setDisabled(true);
			} else {
				jurusan.setDisabled(false);
			}
		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowFakultas.setVisible(!prestasiLuarKampus.isChecked());
				rowJurusan.setVisible(!prestasiLuarKampus.isChecked());
			}
		};

		prestasiLuarKampus.addEventListener("onClick", eventListener);
		eventListener.onEvent(null);

		Common.generateTahunAjaran(tahunAkademik = new Combobox());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, prestasiDosen.getTahunAkademik());

		jenisSemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		jenisSemester.setSelectedIndex(1);
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);

		Common.selectComboItem(jenisSemester, prestasiDosen.getJenisSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(tahun = new Intbox(prestasiDosen.getTahun()));
		tahun.setWidth("90%");
		tahun.setReadonly(true);

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					tahun.setValue(Integer.parseInt(tahunAkademik.getValue().split("/")[0]));
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(prestasiDosen.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Scan / foto sertifikat prestasi *"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, prestasiDosen.getId(), PrestasiDosen.class.getName(),
				"Lampiran Sertifikat", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainDosen = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran kegiatan lebih dari satu file, zip dulu semua file tersebut");

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
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kejuaraan",
					"Kolom Nama Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tanggal.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal Mulai Kejuaraan",
					"Kolom Tanggal Mulai Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal Mulai Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tanggalSelesai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal Selesai Kejuaraan",
					"Kolom Tanggal Selesai Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal Selesai Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (dosen.getAttribute("dosen") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Dosen",
					"Kolom Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tempat.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tempat Kejuaraan",
					"Kolom Tempat Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tempat Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (juara.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Juara ke,",
					"Kolom Juara ke, belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Juara ke,.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (penyelenggara.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Penyelenggara",
					"Kolom Penyelenggara belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Penyelenggara.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nomorSertifikat.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nomor sertifikat kejuaraan",
					"Kolom Nomor sertifikat kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nomor sertifikat kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (cabangPrestasiDosen.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Cabang Kejuaraan",
					"Kolom Cabang Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Cabang Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (kategoriPrestasiDosen.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kategori Kejuaraan",
					"Kolom Kategori Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kategori Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (jumlahPeserta.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jumlah peserta kejuaraan",
					"Kolom Jumlah peserta kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jumlah peserta kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (capaian.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Capaian kejuaraan",
					"Kolom Capaian kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Capaian kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (!prestasiLuarKampus.isChecked()
				&& (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show(
					"Jika prestasi di dalam kampus, maka data " + Common.getBahasaConfig("Fakultas") + " harus diisi",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		try {

			if (prestasiDosen != null && prestasiDosen.getId() != null) {

				LampiranLain lam = LampiranLain.ambil(prestasiDosen.getId(), PrestasiDosen.class.getName());
				if (lam == null) {
					MyMessageboxConfig.show("File scan / foto sertifikat prestasi harus diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			} else {
				if (lainDosen == null) {
					MyMessageboxConfig.show("File scan / foto sertifikat prestasi harus diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			}

		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/PrestasiDosenAction.java:1244");
		}

		Session session = HibernateUtil.currentSession();
		if (prestasiDosen.getId() != null) {
			prestasiDosen = (PrestasiDosen) session.load(PrestasiDosen.class, prestasiDosen.getId());

		}

		// private Combobox cabangPrestasiDosen;
		// private Combobox kategoriPrestasiDosen;
		// private Textbox jumlahPeserta;
		// private Textbox capaian;
		// private Textbox url;

		prestasiDosen.setPeringkat(peringkat.getValue());
		prestasiDosen.setCabangPrestasiDosen((CabangPrestasiDosen) (cabangPrestasiDosen.getSelectedItem() == null ? null
				: cabangPrestasiDosen.getSelectedItem().getValue()));
		prestasiDosen.setKategoriPrestasiDosen(
				(KategoriPrestasiDosen) (kategoriPrestasiDosen.getSelectedItem() == null ? null
						: kategoriPrestasiDosen.getSelectedItem().getValue()));
		prestasiDosen.setJumlahPeserta(jumlahPeserta.getValue());
		prestasiDosen.setCapaian(capaian.getValue());
		prestasiDosen.setUrl(url.getValue());

		prestasiDosen.setPrestasiLuarKampus(prestasiLuarKampus.isChecked());
		prestasiDosen.setTanggal(tanggal.getValue());
		prestasiDosen.setTanggalSelesai(tanggalSelesai.getValue());
		prestasiDosen.setNama(nama.getValue());
		prestasiDosen.setNamaEn(namaEn.getValue());
		prestasiDosen.setTempat(tempat.getValue());
		prestasiDosen.setJuara(juara.getValue());
		prestasiDosen.setNomorSertifikat(nomorSertifikat.getValue());
		prestasiDosen.setDosen((Dosen) dosen.getAttribute("dosen"));
		prestasiDosen.setKeterangan(keterangan.getValue());
		prestasiDosen.setPenyelenggara(penyelenggara.getValue());
		prestasiDosen.setTanggal(tanggal.getValue());

		prestasiDosen.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		prestasiDosen.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		prestasiDosen.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		prestasiDosen.setJenisSemester((String) jenisSemester.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, prestasiDosen);

		if (lainDosen != null && lainDosen.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainDosen);
				lainDosen.setRef(prestasiDosen.getId());

				session.getTransaction().begin();
				session.update(lainDosen);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Long loginAtasan = tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen") ? tbmuser.getDosen().getId() : null;

		System.out.println("loginAtasan => " + loginAtasan);

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PrestasiDosen.class)

				.createAlias("dosen", "dosen").createAlias("dosen.jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add((searchdosen == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchdosen.getAttribute("dosen") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("dosen", searchdosen.getAttribute("dosen")),
								Restrictions.eq("dosen.atasanlangsung", loginAtasan))))

				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tanggal", searchmulai.getValue())))

				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.le("tanggal", searchsampai.getValue())));

		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchpenyelenggara.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("penyelenggara", searchpenyelenggara.getValue().trim(),
								MatchMode.ANYWHERE))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						|| searchstatus.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.add(searchcabangPrestasiDosen.getSelectedItem() == null
						|| searchcabangPrestasiDosen.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("cabangPrestasiDosen",
										searchcabangPrestasiDosen.getSelectedItem().getValue()))

				.add(searchkategoriPrestasiDosen.getSelectedItem() == null
						|| searchkategoriPrestasiDosen.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kategoriPrestasiDosen",
										searchkategoriPrestasiDosen.getSelectedItem().getValue()))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", searchta.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("dosen.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PrestasiDosen> myPrestasiDosens;

		if (prestasiDosenSelected != null) {
			myPrestasiDosens = new ArrayList<PrestasiDosen>();
			myPrestasiDosens.add(prestasiDosenSelected);
			myPrestasiDosens.addAll(initCriteria(true).add(Restrictions.ne("id", prestasiDosenSelected.getId()))
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list());
		} else {
			myPrestasiDosens = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		}

		ListModel strset = new SimpleListModel(myPrestasiDosens);
		grid.setRowRenderer(new PrestasiDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
