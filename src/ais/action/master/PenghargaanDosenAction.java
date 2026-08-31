package ais.action.master;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
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

import ais.action.master.helper.AmbilDataDosenBanbox;
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
import ais.database.model.Jurusan;
import ais.database.model.KategoriPenghargaan;
import ais.database.model.Konfigurasi;
import ais.database.model.PenghargaanDosen;
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
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk penghargaan dosen. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code AmbilDataDosenBanbox searchdosen}, {@code
 * MyDatebox searchmulai}, {@code MyDatebox searchsampai}, {@code Combobox searchstatus}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code getDspace()}, {@code getDspaceTipePenghargaanDosen()}, {@code onSearchDefault()});
 * mutasi data ({@code onSave()}); operasi domain lain ({@code onKategoriPenghargaan()}, {@code onAdd()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PenghargaanDosenAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataDosenBanbox searchdosen;
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
	private AmbilDataDosenBanbox dosen;
	private Combobox jurusan;
	private Combobox fakultas;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Textbox keterangan;

	private PenghargaanDosen penghargaanDosen;
	private MyToolbarbuttonConfig add;

	protected LampiranLain lainDosen;
	private Tbmuser tbmuser;
	private Textbox nomorSertifikat;

	private Combobox kategoriPenghargaan;
	private Textbox capaian;
	private Textbox url;

	private Dosen mhs;
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
	private PenghargaanDosen penghargaanDosenSelected = null;
	private Textbox namaEn;

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

		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
				&& colNama != null) {
			colNama.setWidth("0px");
		}

		Common.generateTahunAjaranDanSemua(searchta);
		Common.selectComboItem(searchta, null);

		kategoriPenghargaanTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.ambilDosen() == null);

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

		Comboitem comboitem = new Comboitem(PenghargaanDosen.BELUM_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PenghargaanDosen.BELUM_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PenghargaanDosen.SEDANG_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PenghargaanDosen.SEDANG_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PenghargaanDosen.DISETUJUI);
		if (comboitem != null) { comboitem.setValue(PenghargaanDosen.DISETUJUI); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PenghargaanDosen.DITOLAK);
		if (comboitem != null) { comboitem.setValue(PenghargaanDosen.DITOLAK); }
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

		if (execution.getParameter("penghargaan") != null) {
			penghargaanDosenSelected = (PenghargaanDosen) GeneralValueObject.ambilData(PenghargaanDosen.class,
					execution.getParameter("penghargaan").toString());
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
			searchdosen.setAttribute("dosen", mhs);
			searchdosen.setDisabled(true);
			searchdosen.setValue(mhs.getNama());
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

		String[] contents = new String[] { "id", "dosen", "nama", "tanggal", "tanggalSelesai", "nomorSertifikat",
				"kategoriPenghargaan", "capaian", "url", "fakultas", "jurusan", "tahunAkademik", "jenisSemester",
				"tahun", "status", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PenghargaanDosen.class, contents);
		upload.setVisible(
				(add != null && add.isVisible()) && tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.ambilDosen() == null);
		Common.appendKeToolbar(upload, add, comp);

		if (mhs != null) {

		}

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		Common.appendKeToolbar(exportKeOjs, add, comp);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("karya_dosen_terhubung_ke_dspace"));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Intbox intbox = new Intbox(0);
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (intbox.getValue() == 0) {
							MyMessageboxConfig.show(
									"Data tidak ditemukan, khusus untuk penghargaan dosen, dosen harus mempunya HOMEBASE PRODI terlebih dahulu sebelum bisa mempublikasikan ke dalam repository",
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
							List<PenghargaanDosen> penghargaanDosens = initCriteria(true)
									.add(Restrictions.eq("status", PenghargaanDosen.DISETUJUI)).list();
							intbox.setValue(penghargaanDosens.size());

							int rowIndex = 1;
							for (PenghargaanDosen penghargaanDosen : penghargaanDosens) {
								label.setValue("Sedang memproses data " + penghargaanDosen.toString() + " ("
										+ Common.numberFormat.get().format((rowIndex++) * 100.0 / penghargaanDosens.size())
										+ " %)");
								PenghargaanDosenAction.getDspace(cookie, penghargaanDosen, true);
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
				&& Common.bolehKonfigurasi("karya_dosen_terhubung_ke_dspace"));
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
												List<PenghargaanDosen> penghargaanDosens = initCriteria(true)
														.createAlias("dosen", "dosen")
														.add(Restrictions.isNotNull("dosen.jurusan"))
														.add(Restrictions.eq("status", PenghargaanDosen.DISETUJUI))
														.list();

												int rowIndex = 1;
												for (PenghargaanDosen penghargaanDosen : penghargaanDosens) {
													label.setValue("Sedang memproses data "
															+ penghargaanDosen.toString() + " ("
															+ Common.numberFormat.get().format(
																	(rowIndex++) * 100.0 / penghargaanDosens.size())
															+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(PenghargaanDosen.class.getName(),
																	penghargaanDosen.getId());
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

		if (tbmuser != null && tbmuser.getDosen() == null) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Setujui Semua", "/img/svg/check2.svg");
			button.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_setujui_semua_karya_dosen"));
			Common.appendKeToolbar(button, add, comp);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin melakukan persetujuan semua karya dosen ini ?",
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
												try {
												double persenVeridikasi = 0.0;
												@SuppressWarnings("unchecked")
												List<PenghargaanDosen> penghargaanDosens = initCriteria(true)
														.add(Restrictions.ne("status", PenghargaanDosen.DITOLAK))
														.list();
												int size = penghargaanDosens.size();
												int iverifikasi = 0;
												for (PenghargaanDosen penghargaanDosen : penghargaanDosens) {
													iverifikasi++;
													try {
														persenVeridikasi = iverifikasi * 100.0 / size;
														if (label != null) {
															label.setValue(Common.numberFormat.get().format(persenVeridikasi)
																	+ "% .. Proses Persetujuan "
																	+ penghargaanDosen.getNama());

														}

														Session session = HibernateUtil.currentNativeSession();

														penghargaanDosen.setStatus(PenghargaanDosen.DISETUJUI);
														session.getTransaction().begin();
														Common.refreshUpdate(session, penghargaanDosen);
														session.getTransaction().commit();

														// session.disconnect();
														if (session.isOpen()) {session.disconnect();session.close();}
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
													HibernateUtil.closeSession();
												}
												label.setValue("");
																							} finally {
													ais.database.hibernate.HibernateUtil.closeSession();
												}
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
													MyMessageboxConfig.show("Persetujuan karya dosen telah selesai",
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

	public static DspaceInformation getDspace(String cookie, PenghargaanDosen penghargaanDosen, boolean update)
			throws Exception {

		JSONArray jsonArray = new JSONArray();

		String nama = "";
		if (penghargaanDosen.getDosen() != null) {
			nama = penghargaanDosen.getDosen().getNama();
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
		parser.parse(new StringReader(penghargaanDosen.getCapaian()));

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", parser.getText());
		jsonArray.put(jsonMetadata);

		if (penghargaanDosen.getKategoriPenghargaan() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.type");
			jsonMetadata.put("value", penghargaanDosen.getKategoriPenghargaan().getNama());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", penghargaanDosen.getNama());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.subject");
		jsonMetadata.put("value", penghargaanDosen.getKeterangan());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.uri");
		jsonMetadata.put("value", penghargaanDosen.getUrl());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.issn");
		jsonMetadata.put("value", penghargaanDosen.getNomorSertifikat());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.language");
		jsonMetadata.put("value", penghargaanDosen.getDosen().getBahasa());
		jsonArray.put(jsonMetadata);

		if (penghargaanDosen.getTanggal() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(penghargaanDosen.getTanggal()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain lam = LampiranLain.ambil(penghargaanDosen.getId(), PenghargaanDosen.class.getName());
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

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, penghargaanDosen,
				jsonPost.toString(), jsonArray.toString(), update, "items",
				"collections/" + getDspaceTipePenghargaanDosen(cookie, penghargaanDosen) + "/items",
				"items/{uuid}/metadata");

		if (lam != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lam, "Sertifikat / Lampiran Bukti Karya");
		}

		return dspaceInformation;
	}

	public static DspaceInformation getDspaceTipePenghargaanDosen(String cookie, PenghargaanDosen penghargaanDosen)
			throws Exception {
		Jurusan jurusan = penghargaanDosen.getDosen().getJurusan();

		String description = "Karya dosen untuk " + Common.getBahasaConfig("Jurusan") + " "
				+ penghargaanDosen.getDosen().getJurusan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Karya Dosen");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription",
				"Karya Dosen " + penghargaanDosen.getDosen().getJurusan().getJenjang().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common
				.getKonfigurasi("dspace_label_collection_penghargaanDosen_" + jurusan.getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");

	}

	class PenghargaanDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenghargaanDosen penghargaanDosen = (PenghargaanDosen) arg1;

			try {
				if (penghargaanDosenSelected != null
						&& penghargaanDosenSelected.getId().equals(penghargaanDosen.getId())) {
					arg0.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PenghargaanDosenAction.java:656");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);

			Vbox myvbox = new Vbox();
			myvbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(penghargaanDosen.getDosen()).setParent(myvbox);

			new Label(penghargaanDosen.getDosen().getNidn() + "-" + penghargaanDosen.getDosen().getNama())
					.setParent(myvbox);

			Vbox a = RevisiHelper.createNewRevisi(PenghargaanDosen.class, penghargaanDosen, penghargaanDosen.getNama());
			a.setParent(arg0);

			new Label(penghargaanDosen.getNamaEn()).setParent(a);

			myvbox = new Vbox();
			myvbox.setParent(detail);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, penghargaanDosen.getId(), PenghargaanDosen.class.getName(),
					"Lampiran", false, null, null, false, false, false, false);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new MyLabelAgakKecil("Tanggal: "
					+ (penghargaanDosen.getTanggal() == null ? ""
							: Common.dateFormat1.get().format(penghargaanDosen.getTanggal()))
					+ (penghargaanDosen.getTanggalSelesai() == null ? ""
							: " s.d " + Common.dateFormat1.get().format(penghargaanDosen.getTanggalSelesai())))
					.setParent(myvbox);
			new MyLabelAgakKecil(
					"TA/Smt: " + penghargaanDosen.getTahunAkademik() + "/" + penghargaanDosen.getJenisSemester())
					.setParent(myvbox);

			myvbox = new Vbox();
			myvbox.setParent(arg0);

			new MyLabelAgakKecil("Kategori: " + (penghargaanDosen.getKategoriPenghargaan() == null ? ""
					: penghargaanDosen.getKategoriPenghargaan().getNama())).setParent(myvbox);
			new MyLabelAgakKecil("Link: " + penghargaanDosen.getUrl()).setParent(myvbox);

			new Label(penghargaanDosen.getCapaian()).setParent(arg0);

			new Label(penghargaanDosen.getNomorSertifikat()).setParent(arg0);

			// Kolom aksi rapi: semua tombol dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi.
			// aksiBoxRef menampung Vbox pembungkus supaya visibilitas grup tetap bisa
			// diubah dari listener Combobox status (perilaku sama dgn Hbox toolbar lama).
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			final Vbox[] aksiBoxRef = new Vbox[1];

			if (mhs == null && tbmuser != null) {
				final Combobox status = new Combobox();
				Comboitem comboitem = new Comboitem(PenghargaanDosen.BELUM_DIPROSES);
				comboitem.setValue(PenghargaanDosen.BELUM_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PenghargaanDosen.SEDANG_DIPROSES);
				comboitem.setValue(PenghargaanDosen.SEDANG_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PenghargaanDosen.DISETUJUI);
				comboitem.setValue(PenghargaanDosen.DISETUJUI);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PenghargaanDosen.DITOLAK);
				comboitem.setValue(PenghargaanDosen.DITOLAK);
				status.appendChild(comboitem);

				Common.selectComboItem(status, penghargaanDosen.getStatus());
				status.setParent(arg0);
				status.setReadonly(true);
				status.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						penghargaanDosen.setStatus((String) (status.getSelectedItem() == null
								|| status.getSelectedItem().getValue() == null ? null
										: status.getSelectedItem().getValue()));
						Common.refreshUpdate(penghargaanDosen);
						if (aksiBoxRef[0] != null) {
							aksiBoxRef[0].setVisible(!penghargaanDosen.getStatus().equals(PenghargaanDosen.DISETUJUI));
						}
					}
				};
				status.addEventListener("onChange", eventListener);
			} else {
				new Label(penghargaanDosen.getStatus()).setParent(arg0);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil(
					penghargaanDosen.getFakultas() == null ? "Semua" : penghargaanDosen.getFakultas().getNama())
					.setParent(vbox);
			new MyLabelAgakKecil(
					penghargaanDosen.getJurusan() == null ? "Semua" : penghargaanDosen.getJurusan().getNama())
					.setParent(vbox);

			new Label(penghargaanDosen.getKeterangan()).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(penghargaanDosen);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

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

											if (penghargaanDosenSelected != null && penghargaanDosenSelected.getId()
													.equals(penghargaanDosen.getId())) {
												penghargaanDosenSelected = null;
											}

											Common.refreshDelete(penghargaanDosen);
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

			aksiBoxRef[0] = ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
			aksiBoxRef[0].setVisible(!penghargaanDosen.getStatus().equals(PenghargaanDosen.DISETUJUI) && tbmuser != null);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PenghargaanDosen());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final PenghargaanDosen penghargaanDosen) throws Exception {
		this.penghargaanDosen = penghargaanDosen;
		addWindow.setTitle(penghargaanDosen.getId() == null ? "Tambah Penghargaan Dosen" : "Ubah Penghargaan Dosen");
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
		row.appendChild(nama = new Textbox(penghargaanDosen.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Karya (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(penghargaanDosen.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pendaftaran Karya *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		tanggal = new MyDatebox(penghargaanDosen.getTanggal());
		hbox.appendChild(tanggal);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		tanggalSelesai = new MyDatebox(penghargaanDosen.getTanggalSelesai());
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
			dosen.setAttribute("dosen", penghargaanDosen.getDosen());
			dosen.setValue(penghargaanDosen.getDosen() == null ? "" : penghargaanDosen.getDosen().getNama());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Sertifikat Penghargaan *"));
		row.appendChild(nomorSertifikat = new Textbox(penghargaanDosen.getNomorSertifikat()));
		nomorSertifikat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bentuk Penghargaan *"));
		row.appendChild(kategoriPenghargaan = new Combobox());
		Common.insertCombo(kategoriPenghargaan, "nama", KategoriPenghargaan.class);
		Common.selectComboItem(kategoriPenghargaan, penghargaanDosen.getKategoriPenghargaan());
		kategoriPenghargaan.setWidth("90%");
		kategoriPenghargaan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Capaian *"));
		row.appendChild(capaian = new Textbox(penghargaanDosen.getCapaian()));
		capaian.setWidth("90%");
		capaian.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link / URL"));
		row.appendChild(url = new Textbox(penghargaanDosen.getUrl()));
		url.setWidth("90%");
		url.setRows(2);

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		if (penghargaanDosen.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			penghargaanDosen.setFakultas(tbmuser.ambilFakultas());
		}
		rowFakultas = new MyFormRow();
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(fakultas, penghargaanDosen.getFakultas());
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
		Common.pilihJurusan(jurusan, penghargaanDosen.getJurusan());

		if (penghargaanDosen.getJurusan() == null) {
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

		Common.generateTahunAjaran(tahunAkademik = new Combobox());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, penghargaanDosen.getTahunAkademik());

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

		Common.selectComboItem(jenisSemester, penghargaanDosen.getJenisSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(tahun = new Intbox(penghargaanDosen.getTahun()));
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
		row.appendChild(keterangan = new Textbox(penghargaanDosen.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Scan / foto sertifikat penghargaan *"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, penghargaanDosen.getId(), PenghargaanDosen.class.getName(),
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

			if (penghargaanDosen != null && penghargaanDosen.getId() != null) {
				LampiranLain lam = LampiranLain.ambil(penghargaanDosen.getId(), PenghargaanDosen.class.getName());

				if (lam == null) {
					MyMessageboxConfig.show("File scan / foto sertifikat penghargaan harus diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			} else {
				if (lainDosen == null) {
					MyMessageboxConfig.show("File scan / foto sertifikat penghargaan harus diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			}

		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/PenghargaanDosenAction.java:1120");
		}

		Session session = HibernateUtil.currentSession();
		if (penghargaanDosen.getId() != null) {
			penghargaanDosen = (PenghargaanDosen) session.load(PenghargaanDosen.class, penghargaanDosen.getId());

		}

		// private Combobox cabangPenghargaanDosen;
		// private Combobox kategoriPenghargaan;
		// private Textbox jumlahPeserta;
		// private Textbox capaian;
		// private Textbox url;

		penghargaanDosen
				.setKategoriPenghargaan((KategoriPenghargaan) (kategoriPenghargaan.getSelectedItem() == null ? null
						: kategoriPenghargaan.getSelectedItem().getValue()));
		penghargaanDosen.setCapaian(capaian.getValue());
		penghargaanDosen.setUrl(url.getValue());

		penghargaanDosen.setTanggal(tanggal.getValue());
		penghargaanDosen.setTanggalSelesai(tanggalSelesai.getValue());
		penghargaanDosen.setNama(nama.getValue());
		penghargaanDosen.setNamaEn(namaEn.getValue());
		penghargaanDosen.setNomorSertifikat(nomorSertifikat.getValue());
		penghargaanDosen.setDosen((Dosen) dosen.getAttribute("dosen"));
		penghargaanDosen.setKeterangan(keterangan.getValue());
		penghargaanDosen.setTanggal(tanggal.getValue());

		penghargaanDosen.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		penghargaanDosen.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		penghargaanDosen.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		penghargaanDosen.setJenisSemester((String) jenisSemester.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, penghargaanDosen);

		if (lainDosen != null && lainDosen.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainDosen);
				lainDosen.setRef(penghargaanDosen.getId());

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

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenghargaanDosen.class)

				.createAlias("dosen", "dosen").createAlias("dosen.jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add((searchdosen == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchdosen.getAttribute("dosen") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("dosen", searchdosen.getAttribute("dosen"))))

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
						|| (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen"))
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("dosen.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen"))
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenghargaanDosen> myPenghargaanDosens;

		if (penghargaanDosenSelected != null) {
			myPenghargaanDosens = new ArrayList<PenghargaanDosen>();
			myPenghargaanDosens.add(penghargaanDosenSelected);
			myPenghargaanDosens.addAll(initCriteria(true).add(Restrictions.ne("id", penghargaanDosenSelected.getId()))
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list());
		} else {
			myPenghargaanDosens = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		}

		ListModel strset = new SimpleListModel(myPenghargaanDosens);
		grid.setRowRenderer(new PenghargaanDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
