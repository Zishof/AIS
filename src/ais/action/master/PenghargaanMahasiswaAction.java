package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
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
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.Html2Text;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisAktfitasMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KategoriPenghargaan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PenghargaanMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class PenghargaanMahasiswaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataMahasiswaBanbox searchmahasiswa;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;
	private Combobox searchstatus;
	private Combobox searchta;
	private Combobox searchjurusan;
	private Combobox searchfakultas;
	private Combobox searchkategoriPenghargaan;

	private Textbox nama;
	private MyDatebox tanggal;
	private MyDatebox tanggalSelesai; 
	private AmbilDataMahasiswaBanbox mahasiswa;
	private Combobox jurusan;
	private Combobox fakultas;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Textbox keterangan;

	private PenghargaanMahasiswa penghargaanMahasiswa;
	private MyToolbarbuttonConfig add;

	protected LampiranLain lainMahasiswa;
	private Tbmuser tbmuser;
	private Textbox nomorSertifikat;

	private Combobox kategoriPenghargaan;
	private Textbox capaian;
	private Textbox url;

	private Mahasiswa mhs;
	private Row rowFakultas;
	private Row rowJurusan;

	private Tabpanel kategoriPenghargaanTab;

	private MyColumnConfig colNama;

	public void onKategoriPenghargaan(Event event) {
		if (kategoriPenghargaanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kategoriPenghargaanTab);
			MyInclude iframe = new MyInclude("/pages/master/kategori_penghargaan.zul");
			iframe.setParent(window);
		}
	}

	private Intbox tahun;
	private PenghargaanMahasiswa penghargaanMahasiswaSelected = null;
	private Textbox namaEn;
	private Combobox jenisAktfitasMahasiswa;
	private Textbox alamat;
	private Textbox noSk;
	private MyDatebox tglSk;
	private AmbilDataDosenBanbox dosenPembina1;
	private AmbilDataDosenBanbox dosenPembina2;

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
		
		if (tbmuser != null && tbmuser.getMahasiswa() != null && colNama != null) {
			colNama.setWidth("0px");
		}

		Common.generateTahunAjaranDanSemua(searchta);
		Common.selectComboItem(searchta, null);

		kategoriPenghargaanTab.getLinkedTab().setVisible(tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null);

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(KategoriPenghargaan.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			KategoriPenghargaan kategoriPenghargaan = new KategoriPenghargaan();
			kategoriPenghargaan.setNama("Paten");
			session.save(kategoriPenghargaan);

			kategoriPenghargaan = new KategoriPenghargaan();
			kategoriPenghargaan.setNama("HaKI");
			session.save(kategoriPenghargaan);

			kategoriPenghargaan = new KategoriPenghargaan();
			kategoriPenghargaan.setNama("Nasional / Internasional");
			session.save(kategoriPenghargaan);
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.insertComboDanSemua(searchkategoriPenghargaan, "nama", KategoriPenghargaan.class);

		Comboitem comboitem = new Comboitem(PenghargaanMahasiswa.BELUM_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PenghargaanMahasiswa.BELUM_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PenghargaanMahasiswa.SEDANG_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PenghargaanMahasiswa.SEDANG_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PenghargaanMahasiswa.DISETUJUI);
		if (comboitem != null) { comboitem.setValue(PenghargaanMahasiswa.DISETUJUI); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PenghargaanMahasiswa.DITOLAK);
		if (comboitem != null) { comboitem.setValue(PenghargaanMahasiswa.DITOLAK); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchstatus.appendChild(comboitem);
		if (searchstatus != null) { searchstatus.setReadonly(true); }
		if (searchstatus != null) { searchstatus.setSelectedItem(comboitem); }

		searchmahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (execution.getParameter("mahasiswa") != null) {
			mhs = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("mahasiswa")))).uniqueResult();
		} else {
			mhs = tbmuser == null ? null : tbmuser.getMahasiswa();
		}

		if (execution.getParameter("penghargaan") != null) {
			penghargaanMahasiswaSelected = (PenghargaanMahasiswa) GeneralValueObject
					.ambilData(PenghargaanMahasiswa.class, execution.getParameter("penghargaan").toString());
		}

		if (execution.getParameter("tahunAjaran") != null) {
			String tahunAjaran = execution.getParameter("tahunAjaran");
			Common.selectComboItem(true, searchta, tahunAjaran);
		}

		if (execution.getParameter("kategoriPenghargaan") != null) {
			KategoriPenghargaan kategoriPenghargaanSelected = (KategoriPenghargaan) HibernateUtil.currentSession()
					.createCriteria(KategoriPenghargaan.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("kategoriPenghargaan"))))
					.uniqueResult();
			Common.selectComboItem(true, searchkategoriPenghargaan, kategoriPenghargaanSelected);
		}

		if (execution.getParameter("jurusan") != null) {
			Jurusan jurusanSelected = (Jurusan) HibernateUtil.currentSession().createCriteria(Jurusan.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("jurusan")))).uniqueResult();
			Common.selectComboItem(true, searchjurusan, jurusanSelected);

			Common.selectComboItem(true, searchfakultas,
					jurusanSelected == null ? null : jurusanSelected.getFakultas());

		}

		if (mhs != null) {
			searchmahasiswa.setAttribute("mahasiswa", mhs);
			searchmahasiswa.setDisabled(true);
			searchmahasiswa.setValue(mhs.getNama());
		}

		if (add != null) { add.setVisible(tbmuser != null); }
		// add.setTooltiptext("Tambah");

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "mahasiswa", "nama", "tanggal", "tanggalSelesai", "nomorSertifikat",
				"kategoriPenghargaan", "capaian", "dosenPembina1", "dosenPembina2", "url", "fakultas", "jurusan",
				"tahunAkademik", "jenisSemester", "tahun", "status", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PenghargaanMahasiswa.class, contents);
		upload.setVisible((add != null && add.isVisible()) && tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null);
		Common.appendKeToolbar(upload, add, comp);

		if (mhs != null) {

		}

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		Common.appendKeToolbar(exportKeOjs, add, comp);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("karya_mahasiswa_terhubung_ke_dspace"));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Intbox intbox = new Intbox(0);
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (intbox.getValue() == 0) {
							MyMessageboxConfig.show(
									"Data tidak ditemukan, khusus untuk penghargaan mahasiswa, mahasiswa harus mempunya HOMEBASE PRODI terlebih dahulu sebelum bisa mempublikasikan ke dalam repository",
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
							List<PenghargaanMahasiswa> penghargaanMahasiswas = initCriteria(true)
									.add(Restrictions.eq("status", PenghargaanMahasiswa.DISETUJUI)).list();
							intbox.setValue(penghargaanMahasiswas.size());

							int rowIndex = 1;
							for (PenghargaanMahasiswa penghargaanMahasiswa : penghargaanMahasiswas) {
								label.setValue("Sedang memproses data " + penghargaanMahasiswa.toString() + " ("
										+ Common.numberFormat.get()
												.format((rowIndex++) * 100.0 / penghargaanMahasiswas.size())
										+ " %)");
								PenghargaanMahasiswaAction.getDspace(cookie, penghargaanMahasiswa, true);
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
				&& Common.bolehKonfigurasi("karya_mahasiswa_terhubung_ke_dspace"));
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
												List<PenghargaanMahasiswa> penghargaanMahasiswas = initCriteria(true)
														.createAlias("mahasiswa", "mahasiswa")
														.add(Restrictions.isNotNull("mahasiswa.jurusan"))
														.add(Restrictions.eq("status", PenghargaanMahasiswa.DISETUJUI))
														.list();

												int rowIndex = 1;
												for (PenghargaanMahasiswa penghargaanMahasiswa : penghargaanMahasiswas) {
													label.setValue("Sedang memproses data "
															+ penghargaanMahasiswa.toString() + " ("
															+ Common.numberFormat.get().format(
																	(rowIndex++) * 100.0 / penghargaanMahasiswas.size())
															+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(PenghargaanMahasiswa.class.getName(),
																	penghargaanMahasiswa.getId());
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

		if (tbmuser != null && tbmuser.getMahasiswa() == null) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Setujui Semua", "/img/svg/check2.svg");
			button.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_setujui_semua_karya_mahasiswa"));
			Common.appendKeToolbar(button, add, comp);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin melakukan persetujuan semua karya mahasiswa ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										final Label label = new Label(
												ais.common.Common.getBahasaConfig("Proses persetujuan sedang berlangsung, harap menunggu.."));

										new Thread(new Runnable() {

											@Override
											public void run() {
												double persenVeridikasi = 0.0;
												@SuppressWarnings("unchecked")
												List<PenghargaanMahasiswa> penghargaanMahasiswas = initCriteria(true)
														.add(Restrictions.ne("status", PenghargaanMahasiswa.DITOLAK))
														.list();
												int size = penghargaanMahasiswas.size();
												int iverifikasi = 0;
												for (PenghargaanMahasiswa penghargaanMahasiswa : penghargaanMahasiswas) {
													iverifikasi++;
													try {
														persenVeridikasi = iverifikasi * 100.0 / size;
														if (label != null) {
															label.setValue(Common.numberFormat.get().format(persenVeridikasi)
																	+ "% .. Proses Persetujuan "
																	+ penghargaanMahasiswa.getNama());

														}

														// Pakai refreshUpdate mandiri (mengelola sesi + transaksi sendiri). Pola lama
														// begin/commit pada native session ThreadLocal rawan "Session is closed!" bila
														// helper bersarang menutup sesi yang sama di tengah proses (thread latar).
														penghargaanMahasiswa.setStatus(PenghargaanMahasiswa.DISETUJUI);
														Common.refreshUpdate(penghargaanMahasiswa);
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
												}
												label.setValue("");
											}
										}).start();

										final Timer timer = new Timer(500);
										timer.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										timer.setRepeats(true);
										timer.addEventListener("onTimer", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Clients.showBusy(label.getValue());
												if (label.getValue().isEmpty()) {
													Clients.clearBusy();
													MyMessageboxConfig.show("Persetujuan karya mahasiswa telah selesai",
															"Pemberitahuan", MyMessageboxConfig.OK,
															MyMessageboxConfig.INFORMATION);
													timer.detach();
													onSearchDefault(arg0);
												}

											}
										});
										timer.start();

									}

								}
							});
				}

			});
		}
	        FilterLanjutHelper.setup(comp);
}

	public static DspaceInformation getDspace(String cookie, PenghargaanMahasiswa penghargaanMahasiswa, boolean update)
			throws Exception {

		JSONArray jsonArray = new JSONArray();

		String nama = "";
		if (penghargaanMahasiswa.getMahasiswa() != null) {
			nama = penghargaanMahasiswa.getMahasiswa().getNama();
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
		parser.parse(new StringReader(penghargaanMahasiswa.getCapaian()));

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", parser.getText());
		jsonArray.put(jsonMetadata);

		if (penghargaanMahasiswa.getKategoriPenghargaan() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.type");
			jsonMetadata.put("value", penghargaanMahasiswa.getKategoriPenghargaan().getNama());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", penghargaanMahasiswa.getNama());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.subject");
		jsonMetadata.put("value", penghargaanMahasiswa.getKeterangan());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.uri");
		jsonMetadata.put("value", penghargaanMahasiswa.getUrl());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.issn");
		jsonMetadata.put("value", penghargaanMahasiswa.getNomorSertifikat());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.language");
		jsonMetadata.put("value", penghargaanMahasiswa.getMahasiswa().getBahasa());
		jsonArray.put(jsonMetadata);

		if (penghargaanMahasiswa.getTanggal() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(penghargaanMahasiswa.getTanggal()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain lam = LampiranLain.ambil(penghargaanMahasiswa.getId(), PenghargaanMahasiswa.class.getName());
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

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, penghargaanMahasiswa,
				jsonPost.toString(), jsonArray.toString(), update, "items",
				"collections/" + getDspaceTipePenghargaanMahasiswa(cookie, penghargaanMahasiswa) + "/items",
				"items/{uuid}/metadata");

		if (lam != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lam, "Sertifikat / Lampiran Bukti Karya");
		}

		return dspaceInformation;
	}

	public static DspaceInformation getDspaceTipePenghargaanMahasiswa(String cookie,
			PenghargaanMahasiswa penghargaanMahasiswa) throws Exception {
		Jurusan jurusan = penghargaanMahasiswa.getMahasiswa().getJurusan();

		String description = "Karya mahasiswa untuk " + Common.getBahasaConfig("Jurusan") + " "
				+ penghargaanMahasiswa.getMahasiswa().getJurusan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Karya Mahasiswa");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", "Karya Mahasiswa "
				+ penghargaanMahasiswa.getMahasiswa().getJurusan().getJenjang().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common
				.getKonfigurasi("dspace_label_collection_penghargaanMahasiswa_" + jurusan.getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");

	}

	class PenghargaanMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenghargaanMahasiswa penghargaanMahasiswa = (PenghargaanMahasiswa) arg1;

			try {
				if (penghargaanMahasiswaSelected != null
						&& penghargaanMahasiswaSelected.getId().equals(penghargaanMahasiswa.getId())) {
					arg0.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PenghargaanMahasiswaAction.java:663");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);

			Vbox myvbox = new Vbox();
			myvbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(penghargaanMahasiswa.getMahasiswa()).setParent(myvbox);

			new Label(
					penghargaanMahasiswa.getMahasiswa().getNim() + "-" + penghargaanMahasiswa.getMahasiswa().getNama())
					.setParent(myvbox);

			Vbox a = RevisiHelper.createNewRevisi(PenghargaanMahasiswa.class, penghargaanMahasiswa,
					penghargaanMahasiswa.getNama());

			new Label(penghargaanMahasiswa.getNamaEn()).setParent(a);

			new Label(penghargaanMahasiswa.getJenisAktfitasMahasiswa() == null ? ""
					: penghargaanMahasiswa.getJenisAktfitasMahasiswa().getNama()).setParent(a);

			a.setParent(arg0);

			myvbox = new Vbox();
			myvbox.setParent(detail);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, penghargaanMahasiswa.getId(),
					PenghargaanMahasiswa.class.getName(), "Lampiran", false, null, null, false, false, false, false);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new MyLabelAgakKecil("Tanggal: "
					+ (penghargaanMahasiswa.getTanggal() == null ? ""
							: Common.dateFormat1.get().format(penghargaanMahasiswa.getTanggal()))
					+ (penghargaanMahasiswa.getTanggalSelesai() == null ? ""
							: " s.d " + Common.dateFormat1.get().format(penghargaanMahasiswa.getTanggalSelesai())))
					.setParent(myvbox);
			new MyLabelAgakKecil("TA/Smt: " + penghargaanMahasiswa.getTahunAkademik() + "/"
					+ penghargaanMahasiswa.getJenisSemester()).setParent(myvbox);

			myvbox = new Vbox();
			myvbox.setParent(arg0);

			new MyLabelAgakKecil("Kategori: " + (penghargaanMahasiswa.getKategoriPenghargaan() == null ? ""
					: penghargaanMahasiswa.getKategoriPenghargaan().getNama())).setParent(myvbox);
			new MyLabelAgakKecil("Link: " + penghargaanMahasiswa.getUrl()).setParent(myvbox);

			new Label((penghargaanMahasiswa.getDosenPembina1() == null ? ""
					: penghargaanMahasiswa.getDosenPembina1().getNama())
					+ (penghargaanMahasiswa.getDosenPembina2() == null ? ""
							: ", " + penghargaanMahasiswa.getDosenPembina2().getNama()))
					.setParent(arg0);

			new Label(penghargaanMahasiswa.getCapaian()).setParent(arg0);

			new Label(penghargaanMahasiswa.getNomorSertifikat()).setParent(arg0);

			final Hbox toolbar = new Hbox();
			final MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Krm ke feeder",
					"/img/Finance-Invoice-icon.png");
			final Hbox myHbox = new Hbox();
			myHbox.setVisible(penghargaanMahasiswa.getStatus().equals(PenghargaanMahasiswa.DISETUJUI));
			if (mhs == null && tbmuser != null) {
				final Combobox status = new Combobox();
				Comboitem comboitem = new Comboitem(PenghargaanMahasiswa.BELUM_DIPROSES);
				comboitem.setValue(PenghargaanMahasiswa.BELUM_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PenghargaanMahasiswa.SEDANG_DIPROSES);
				comboitem.setValue(PenghargaanMahasiswa.SEDANG_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PenghargaanMahasiswa.DISETUJUI);
				comboitem.setValue(PenghargaanMahasiswa.DISETUJUI);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PenghargaanMahasiswa.DITOLAK);
				comboitem.setValue(PenghargaanMahasiswa.DITOLAK);
				status.appendChild(comboitem);

				Common.selectComboItem(status, penghargaanMahasiswa.getStatus());
				status.setParent(arg0);
				status.setReadonly(true);
				status.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						penghargaanMahasiswa.setStatus((String) (status.getSelectedItem() == null
								|| status.getSelectedItem().getValue() == null ? null
										: status.getSelectedItem().getValue()));
						Common.refreshUpdate(penghargaanMahasiswa);
						toolbar.setVisible(!penghargaanMahasiswa.getStatus().equals(PenghargaanMahasiswa.DISETUJUI));

						if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
								&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
							buttonTagihan.setVisible(
									penghargaanMahasiswa.getStatus().equals(PenghargaanMahasiswa.DISETUJUI));

						}
						myHbox.setVisible(penghargaanMahasiswa.getStatus().equals(PenghargaanMahasiswa.DISETUJUI));
					}
				};
				status.addEventListener("onChange", eventListener);
			} else {
				new Label(penghargaanMahasiswa.getStatus()).setParent(arg0);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil(
					penghargaanMahasiswa.getFakultas() == null ? "Semua" : penghargaanMahasiswa.getFakultas().getNama())
					.setParent(vbox);
			new MyLabelAgakKecil(
					penghargaanMahasiswa.getJurusan() == null ? "Semua" : penghargaanMahasiswa.getJurusan().getNama())
					.setParent(vbox);

			new Label(penghargaanMahasiswa.getKeterangan()).setParent(arg0);

			toolbar.setVisible(
					!penghargaanMahasiswa.getStatus().equals(PenghargaanMahasiswa.DISETUJUI) && tbmuser != null);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(penghargaanMahasiswa);
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

											if (penghargaanMahasiswaSelected != null && penghargaanMahasiswaSelected
													.getId().equals(penghargaanMahasiswa.getId())) {
												penghargaanMahasiswaSelected = null;
											}

											Common.refreshDelete(penghargaanMahasiswa);

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

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);

			toolbar.setParent(vbox1);

			myHbox.setParent(vbox1);

			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

				if (penghargaanMahasiswa.getFeeder() != null && !penghargaanMahasiswa.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}

				buttonTagihan.setVisible(penghargaanMahasiswa.getStatus().equals(PenghargaanMahasiswa.DISETUJUI));
				buttonTagihan.setStyle("font-size:8px;");
				buttonTagihan.setParent(vbox1);
				buttonTagihan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						MyMessageboxConfig.show("Apakah yakin ingin mengirim ke feeder ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {

											String[] kon = EksporFromFeederAction.koneksi();
											final String ip = kon[0];
											final String port = kon[1];
											final String username = kon[2];
											final String password = kon[3];
											final String url = kon[4];

											if (!EksporFromFeederAction.exists(url)) {

												MyMessageboxConfig.show(
														ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
												return;
											}

											final List<String> errorLog = new ArrayList<String>();

											final Label myLabelProsesDetail = Common
													.displayLoadBar(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															if (arg0 != null && !arg0.getName().isEmpty()) {
																EksporFromFeederAction.display();
																MyMessageboxConfig.show(arg0.getName(), "Info",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);
															}

															if (!errorLog.isEmpty()) {
																String err = "";
																for (String s : errorLog) {
																	err += err.isEmpty() ? s
																			: "\n----------------------------------------------------------------------------------------------------------\n"
																					+ s;
																}

																MyMessageboxConfig.show(err, "Error Terjadi",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.EXCLAMATION);

																File file = new File(Common.REAL_PATH + "/tmp/error_"
																		+ Common.randLong() + ".txt");

																if (!file.getParentFile().exists()) {
																	file.getParentFile().mkdirs();
																}
																FileUtils.writeStringToFile(file, err);
																Filedownload.save(file, "text/plain");
															}

															onSearchDefault(null);
														}
													});

											new Thread(new Runnable() {

												@Override
												public void run() {
													try {
														FeederConnector feederConnector = new FeederConnector(ip,
																Integer.parseInt(port), null);

														String token = feederConnector.getToken(username, password);
														System.out.println("TOKEN => " + token);

														if (token == null || token.trim().isEmpty()
																|| token.trim().toLowerCase().startsWith("error")) {
															myLabelProsesDetail
																	.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
															return;
														}

														FeederExporter feederImporter = new FeederExporter(
																feederConnector, token, null, null, null);
														myLabelProsesDetail
																.setValue("Mengirim data " + penghargaanMahasiswa);

														feederImporter.aktivitasMahasiswaPengahargaan(
																penghargaanMahasiswa, errorLog);

														myLabelProsesDetail.setValue("");
													} catch (Exception e) {
														// FIX "gagal diam-diam": sebelumnya exception di sini hanya
														// dicatat ke log admin lalu progres diset "" (=SUKSES
														// palsu) di luar try, menutupi kegagalan dari pengguna.
														ais.common.Common.tampilErrorJikaAdmin(e);
														myLabelProsesDetail.setValue(
																"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																		"pengiriman data penghargaan mahasiswa \""
																				+ penghargaanMahasiswa + "\" ke Neo Feeder",
																		null, e,
																		new String[] {
																				"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																				"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																				"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
																		.replace("\n", " "));
													}
												}
											}).start();

										}

									}
								});

					}
				});

			}

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PenghargaanMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final PenghargaanMahasiswa penghargaanMahasiswa) throws Exception {
		this.penghargaanMahasiswa = penghargaanMahasiswa;
		addWindow.setTitle(penghargaanMahasiswa.getId() == null ? "Tambah Penghargaan Mahasiswa" : "Ubah Penghargaan Mahasiswa");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Karya *"));
		row.appendChild(nama = new Textbox(penghargaanMahasiswa.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Karya (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(penghargaanMahasiswa.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pendaftaran Karya *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		tanggal = new MyDatebox(penghargaanMahasiswa.getTanggal());
		hbox.appendChild(tanggal);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		tanggalSelesai = new MyDatebox(penghargaanMahasiswa.getTanggalSelesai());
		hbox.appendChild(tanggalSelesai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa *"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setWidth("90%");
		mahasiswa.setReadonly(true);

		if (mhs != null) {
			mahasiswa.setAttribute("mahasiswa", mhs);
			mahasiswa.setDisabled(true);
			mahasiswa.setValue(mhs.getNama());
		} else {
			mahasiswa.setAttribute("mahasiswa", penghargaanMahasiswa.getMahasiswa());
			mahasiswa.setValue(
					penghargaanMahasiswa.getMahasiswa() == null ? "" : penghargaanMahasiswa.getMahasiswa().getNama());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembina I"));
		row.appendChild(dosenPembina1 = new AmbilDataDosenBanbox());
		dosenPembina1.setAttribute("myValue", penghargaanMahasiswa.getDosenPembina1());
		dosenPembina1.setAttribute("dosen", penghargaanMahasiswa.getDosenPembina1());
		dosenPembina1.setValue(penghargaanMahasiswa.getDosenPembina1() == null ? ""
				: penghargaanMahasiswa.getDosenPembina1().getNama());
		dosenPembina1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembina II"));
		row.appendChild(dosenPembina2 = new AmbilDataDosenBanbox());
		dosenPembina2.setAttribute("myValue", penghargaanMahasiswa.getDosenPembina2());
		dosenPembina2.setAttribute("dosen", penghargaanMahasiswa.getDosenPembina2());
		dosenPembina2.setValue(penghargaanMahasiswa.getDosenPembina2() == null ? ""
				: penghargaanMahasiswa.getDosenPembina2().getNama());
		dosenPembina2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Sertifikat Karya *"));
		row.appendChild(nomorSertifikat = new Textbox(penghargaanMahasiswa.getNomorSertifikat()));
		nomorSertifikat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bentuk Karya *"));
		row.appendChild(kategoriPenghargaan = new Combobox());
		Common.insertCombo(kategoriPenghargaan, "nama", KategoriPenghargaan.class);
		Common.selectComboItem(kategoriPenghargaan, penghargaanMahasiswa.getKategoriPenghargaan());
		kategoriPenghargaan.setWidth("90%");
		kategoriPenghargaan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Capaian *"));
		row.appendChild(capaian = new Textbox(penghargaanMahasiswa.getCapaian()));
		capaian.setWidth("90%");
		capaian.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link / URL"));
		row.appendChild(url = new Textbox(penghargaanMahasiswa.getUrl()));
		url.setWidth("90%");
		url.setRows(2);

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		if (penghargaanMahasiswa.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			penghargaanMahasiswa.setFakultas(tbmuser.ambilFakultas());
		}
		rowFakultas = new MyFormRow();
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(fakultas, penghargaanMahasiswa.getFakultas());
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		rowJurusan = new MyFormRow();
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, penghargaanMahasiswa.getJurusan());

		if (penghargaanMahasiswa.getJurusan() == null) {
			if (tbmuser.ambilJurusan() != null
					|| (tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getJurusan() != null)) {
				Common.pilihJurusan(jurusan,
						tbmuser == null || tbmuser.ambilJurusan() == null ? tbmuser.getMahasiswa().getJurusan()
								: tbmuser.ambilJurusan());
				jurusan.setDisabled(true);
			} else {
				jurusan.setDisabled(false);
			}
		}

		Common.generateTahunAjaran(tahunAkademik = new Combobox());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, penghargaanMahasiswa.getTahunAkademik());

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

		Common.selectComboItem(jenisSemester, penghargaanMahasiswa.getJenisSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis / Kampus Merdeka"));
		row.appendChild(jenisAktfitasMahasiswa = new Combobox());
		Common.insertCombo(jenisAktfitasMahasiswa, "nama", "merupakanKampusMerdeka", JenisAktfitasMahasiswa.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisAktfitasMahasiswa, penghargaanMahasiswa.getJenisAktfitasMahasiswa());
		jenisAktfitasMahasiswa.setWidth("90%");
		jenisAktfitasMahasiswa.setReadonly(true);

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			jenisAktfitasMahasiswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(tahun = new Intbox(penghargaanMahasiswa.getTahun()));
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi / Alamat"));
		row.appendChild(alamat = new Textbox(penghargaanMahasiswa.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor SK"));
		row.appendChild(noSk = new Textbox(penghargaanMahasiswa.getNoSk()));
		noSk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK"));
		row.appendChild(tglSk = new MyDatebox(penghargaanMahasiswa.getTglSk()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(penghargaanMahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Scan / foto sertifikat penghargaan *"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, penghargaanMahasiswa.getId(),
				PenghargaanMahasiswa.class.getName(), "Lampiran Sertifikat", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
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
		if (mahasiswa.getAttribute("mahasiswa") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Mahasiswa",
					"Kolom Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Mahasiswa.",
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

		if (kategoriPenghargaan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kategori Kejuaraan",
					"Kolom Kategori Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kategori Kejuaraan.",
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

		try {

			if (penghargaanMahasiswa != null && penghargaanMahasiswa.getId() != null) {

				LampiranLain lam = LampiranLain.ambil(penghargaanMahasiswa.getId(),
						PenghargaanMahasiswa.class.getName());

				if (lam == null) {
					MyMessageboxConfig.show("File scan / foto sertifikat penghargaan harus diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			} else {
				if (lainMahasiswa == null) {
					MyMessageboxConfig.show("File scan / foto sertifikat penghargaan harus diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			}

		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/PenghargaanMahasiswaAction.java:1344");
		}

		Session session = HibernateUtil.currentSession();
		if (penghargaanMahasiswa.getId() != null) {
			penghargaanMahasiswa = (PenghargaanMahasiswa) session.load(PenghargaanMahasiswa.class,
					penghargaanMahasiswa.getId());

		}

		// private Combobox cabangPenghargaanMahasiswa;
		// private Combobox kategoriPenghargaan;
		// private Textbox jumlahPeserta;
		// private Textbox capaian;
		// private Textbox url;

		penghargaanMahasiswa
				.setKategoriPenghargaan((KategoriPenghargaan) (kategoriPenghargaan.getSelectedItem() == null ? null
						: kategoriPenghargaan.getSelectedItem().getValue()));
		penghargaanMahasiswa.setCapaian(capaian.getValue());
		penghargaanMahasiswa.setUrl(url.getValue());

		penghargaanMahasiswa.setTanggal(tanggal.getValue());
		penghargaanMahasiswa.setTanggalSelesai(tanggalSelesai.getValue());
		penghargaanMahasiswa.setNama(nama.getValue());
		penghargaanMahasiswa.setNamaEn(namaEn.getValue());
		penghargaanMahasiswa.setNomorSertifikat(nomorSertifikat.getValue());
		penghargaanMahasiswa.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		penghargaanMahasiswa.setDosenPembina1((Dosen) dosenPembina1.getAttribute("dosen"));
		penghargaanMahasiswa.setDosenPembina2((Dosen) dosenPembina2.getAttribute("dosen"));
		penghargaanMahasiswa.setKeterangan(keterangan.getValue());
		penghargaanMahasiswa.setTanggal(tanggal.getValue());

		penghargaanMahasiswa.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		penghargaanMahasiswa.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		penghargaanMahasiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		penghargaanMahasiswa.setJenisSemester((String) jenisSemester.getSelectedItem().getValue());

		penghargaanMahasiswa.setJenisAktfitasMahasiswa(
				(JenisAktfitasMahasiswa) (jenisAktfitasMahasiswa.getSelectedItem() == null ? null
						: jenisAktfitasMahasiswa.getSelectedItem().getValue()));

		penghargaanMahasiswa.setAlamat(alamat.getValue());
		penghargaanMahasiswa.setNoSk(noSk.getValue());
		penghargaanMahasiswa.setTglSk(tglSk.getValue());

		Common.refreshSaveOrUpdate(session, penghargaanMahasiswa);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(penghargaanMahasiswa.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
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

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenghargaanMahasiswa.class)

				.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")

				.add((searchmahasiswa == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmahasiswa.getAttribute("mahasiswa") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa", searchmahasiswa.getAttribute("mahasiswa"))))

				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tanggal", searchmulai.getValue())))

				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.le("tanggal", searchsampai.getValue())));

		if (order)
			criteria.addOrder(Order.desc("id")); // pengajuan terkini di atas

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						|| searchstatus.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.add(searchkategoriPenghargaan.getSelectedItem() == null
						|| searchkategoriPenghargaan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kategoriPenghargaan",
										searchkategoriPenghargaan.getSelectedItem().getValue()))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", searchta.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| (tbmuser != null && tbmuser.getMahasiswa() != null) ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| (tbmuser != null && tbmuser.getMahasiswa() != null) ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenghargaanMahasiswa> myPenghargaanMahasiswas;

		if (penghargaanMahasiswaSelected != null) {
			myPenghargaanMahasiswas = new ArrayList<PenghargaanMahasiswa>();
			myPenghargaanMahasiswas.add(penghargaanMahasiswaSelected);
			myPenghargaanMahasiswas.addAll(initCriteria(true)
					.add(Restrictions.ne("id", penghargaanMahasiswaSelected.getId()))
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list());
		} else {
			myPenghargaanMahasiswas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		}

		ListModel strset = new SimpleListModel(myPenghargaanMahasiswas);
		grid.setRowRenderer(new PenghargaanMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
