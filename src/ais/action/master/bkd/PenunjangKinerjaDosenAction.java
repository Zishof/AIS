package ais.action.master.bkd;

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
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
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

import ais.action.master.JurusanAction;
import ais.action.master.LogLoginAction;
import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.bkd.helper.PenilaianAsesorHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk penunjang kinerja dosen. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchbukti}, {@code Textbox
 * searchbuktidokumen}, {@code AmbilDataPegawaiBanbox searchpegawai}, {@code Textbox nama};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code getDspaceTipePenunjangKinerjaDosen()}, {@code getDspace()},
 * {@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code displayRow()}, {@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PenunjangKinerjaDosenAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchbukti;
	private Textbox searchbuktidokumen;
	private AmbilDataPegawaiBanbox searchpegawai;

	private Textbox nama;
	private Textbox keterangan;

	private PenunjangKinerjaDosen penunjangKinerjaDosen;

	private Textbox bukti;
	protected LampiranLain buktiPenugasan;
	private MyDoublebox sks;
	private Textbox masaPenugasan;
	private Textbox buktiDokumen;
	protected LampiranLain lampiranBuktiDokumen;
	private Textbox linkDokumen;
	private MyDatebox tanggalMulai;
	private MyDatebox tanggalSampai;
	private AmbilDataPegawaiBanbox pegawai;

	private Boolean ases = false;
	private MyToolbarbuttonConfig add;

	private String jenis = PenunjangKinerjaDosen.PENUNJANG;
	private Combobox tahunAkademik;
	private Combobox semester;
	private Pegawai pegawaiTerpilih;

	private MyColumnConfig detailLbl;
	private MyColumnConfig editLbl;

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

		if (execution.getParameter("pegawai") != null) {
			pegawaiTerpilih = (Pegawai) HibernateUtil.currentSession().createCriteria(Pegawai.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("pegawai").trim()))).uniqueResult();
		}

		if (execution.getParameter("ases") != null) {
			ases = Boolean.parseBoolean(execution.getParameter("ases"));
		}

		if (execution.getParameter("jenis") != null) {
			jenis = execution.getParameter("jenis");
		}

		if (detailLbl != null) { detailLbl.setWidth(ases ? "40px" : "0px"); }
		if (editLbl != null) { editLbl.setWidth(ases ? "0%" : "10%"); }
		if (add != null) { add.setVisible(!ases); }

		if (pegawaiTerpilih != null) {
			searchpegawai.setAttribute("pegawai", pegawaiTerpilih);
			searchpegawai.setValue(pegawaiTerpilih.getNama());
			searchpegawai.setDisabled(true);
		}

		searchpegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		Common.appendKeToolbar(exportKeOjs, add, comp);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("penunjang_kegiatan_terhubung_ke_dspace"));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
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
							String cookie = DspaceCommon.login();
							List<PenunjangKinerjaDosen> penunjangKinerjaDosens = initCriteria(true)
									.createAlias("pegawai", "pegawai").createAlias("pegawai.dosen", "dosen")
									.add(Restrictions.isNotNull("dosen.jurusan")).list();

							int rowIndex = 1;
							for (PenunjangKinerjaDosen penunjangKinerjaDosen : penunjangKinerjaDosens) {
								label.setValue("Sedang memproses data " + penunjangKinerjaDosen.toString() + " ("
										+ Common.numberFormat.get()
												.format((rowIndex++) * 100.0 / penunjangKinerjaDosens.size())
										+ " %)");
								PenunjangKinerjaDosenAction.getDspace(cookie, penunjangKinerjaDosen, true);
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
				&& Common.bolehKonfigurasi("penunjang_kegiatan_terhubung_ke_dspace"));
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
												List<PenunjangKinerjaDosen> penunjangKinerjaDosens = initCriteria(true)
														.createAlias("pegawai", "pegawai")
														.createAlias("pegawai.dosen", "dosen")
														.add(Restrictions.isNotNull("dosen.jurusan")).list();

												int rowIndex = 1;
												for (PenunjangKinerjaDosen penunjangKinerjaDosen : penunjangKinerjaDosens) {
													label.setValue(
															"Sedang memproses data " + penunjangKinerjaDosen.toString()
																	+ " (" + Common.numberFormat.get().format((rowIndex++)
																			* 100.0 / penunjangKinerjaDosens.size())
																	+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(PenunjangKinerjaDosen.class.getName(),
																	penunjangKinerjaDosen.getId());
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

	public static void displayRow(Row arg0, final PenunjangKinerjaDosen penunjangKinerjaDosen, final Boolean ases)
			throws Exception {

		final Vbox vboxKeterangan = new Vbox();
		final EventListener keteranganEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(vboxKeterangan);

				if (penunjangKinerjaDosen.getKeterangan() != null
						&& !penunjangKinerjaDosen.getKeterangan().trim().isEmpty()) {
					new Label("Ket.: " + penunjangKinerjaDosen.getKeterangan()).setParent(vboxKeterangan);
				}

				Session session = HibernateUtil.currentSession();
				@SuppressWarnings("unchecked")
				List<PenilaianAsesor> asesorMemberikanPenilaians = session.createCriteria(PenilaianAsesor.class)
						.add(Restrictions.isNotNull("asesor")).createAlias("asesemenPenilaian", "asesemenPenilaian")
						.add(Restrictions.eq("asesemenPenilaian.penunjangKinerjaDosen", penunjangKinerjaDosen)).list();
				for (PenilaianAsesor penilaianAsesor : asesorMemberikanPenilaians) {
					new Label(penilaianAsesor.getAsesor().getAsesorPenunjangKinerjaDosen().getNama() + " : "
							+ Common.numberFormat.get().format(penilaianAsesor.getSks()) + " sks, "
							+ (penilaianAsesor.getKeterangan())
							+ (penilaianAsesor.getAsesemenPenilaian().getPegawai() == null ? ""
									: " (" + penilaianAsesor.getAsesemenPenilaian().getPegawai().getNama() + ")"))
											.setParent(vboxKeterangan);
				}
			}
		};

		final MyDetail detail = new MyDetail();
		detail.setParent(arg0);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(detail);
				if (detail.isOpen()) {

					PenilaianAsesorHelper.formNilai(penunjangKinerjaDosen.getPegawai(), "penunjangKinerjaDosen",
							penunjangKinerjaDosen, null, penunjangKinerjaDosen.getTahunAkademik(),
							penunjangKinerjaDosen.getSemester(), penunjangKinerjaDosen.getBuktiDokumen(),
							PenilaianAsesor.PENUNJANG_DAN_LAIN_LAIN, keteranganEventListener).setParent(detail);

				}
			}
		};
		detail.addEventListener("onOpen", eventListener);
		if (ases) {
			detail.setOpen(true);
			eventListener.onEvent(null);
		}

		Vbox vbox = new Vbox();
		vbox.setParent(arg0);
		if (penunjangKinerjaDosen.getPegawai() != null) {
			CommonMedia.tampilkanGambarKecil(penunjangKinerjaDosen.getPegawai()).setParent(vbox);
		}
		new Label(penunjangKinerjaDosen.getPegawai() == null ? "" : penunjangKinerjaDosen.getPegawai().getNama())
				.setParent(vbox);

		RevisiHelper
				.createNewRevisi(PenunjangKinerjaDosen.class, penunjangKinerjaDosen, penunjangKinerjaDosen.getNama())
				.setParent(arg0);

		vbox = new Vbox();
		vbox.setParent(arg0);
		new Label(penunjangKinerjaDosen.getBukti()).setParent(vbox);
		Vbox myvbox = new Vbox();
		myvbox.setParent(vbox);

		Hbox hbox = new Hbox();
		hbox.setParent(myvbox);
		LampiranLain.createDownloadUploadFileLain(hbox, penunjangKinerjaDosen.getId(), LampiranLain.BUKTI_PENUGASAN,
				LampiranLain.BUKTI_PENUGASAN, true, null, null, false, false, false, false);

		new Label(penunjangKinerjaDosen.getSks() + " SKS").setParent(arg0);

		vbox = new Vbox();
		vbox.setParent(arg0);
		new Label((penunjangKinerjaDosen.getTanggalMulai() == null ? ""
				: Common.dateFormat4.get().format(penunjangKinerjaDosen.getTanggalMulai()))
				+ (penunjangKinerjaDosen.getTanggalSampai() == null ? ""
						: " s.d " + Common.dateFormat4.get().format(penunjangKinerjaDosen.getTanggalSampai())))
								.setParent(vbox);
		new Label(penunjangKinerjaDosen.getMasaPenugasan()).setParent(vbox);
		new Label("TA : " + penunjangKinerjaDosen.getTahunAkademik()).setParent(vbox);
		new Label("Smt : " + penunjangKinerjaDosen.getSemester()).setParent(vbox);

		vbox = new Vbox();
		vbox.setParent(arg0);
		new Label(penunjangKinerjaDosen.getBuktiDokumen()).setParent(vbox);
		myvbox = new Vbox();
		myvbox.setParent(vbox);

		hbox = new Hbox();
		hbox.setParent(myvbox);
		LampiranLain.createDownloadUploadFileLain(hbox, penunjangKinerjaDosen.getId(), LampiranLain.BUKTI_DOKUMEN,
				LampiranLain.BUKTI_DOKUMEN, true, null, null, false, false, false, false);
		if (penunjangKinerjaDosen.getLinkDokumen() != null
				&& !penunjangKinerjaDosen.getLinkDokumen().trim().isEmpty()) {
			A a;
			(a = new A(penunjangKinerjaDosen.getLinkDokumen())).setParent(vbox);
			a.setHref(penunjangKinerjaDosen.getLinkDokumen());
			a.setTarget("_blank");
		}

		vboxKeterangan.setParent(arg0);
		keteranganEventListener.onEvent(null);
	}

	public static DspaceInformation getDspaceTipePenunjangKinerjaDosen(String cookie,
			PenunjangKinerjaDosen penunjangKinerjaDosen) throws Exception {
		Jurusan jurusan = penunjangKinerjaDosen.getPegawai() == null
				|| penunjangKinerjaDosen.getPegawai().getDosen() == null ? null
						: penunjangKinerjaDosen.getPegawai().getDosen().getJurusan();

		String label_penunjangKinerjaDosen = "Kinerja lain pegawai bidang " + penunjangKinerjaDosen.getJenis();

		String description = label_penunjangKinerjaDosen + " untuk " + Common.getBahasaConfig("Jurusan") + " "
				+ jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", label_penunjangKinerjaDosen);
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", label_penunjangKinerjaDosen + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_penunjangKinerjaDosen_"
				+ jurusan.getId() + "_" + penunjangKinerjaDosen.getJenis(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");

	}

	public static DspaceInformation getDspace(String cookie, PenunjangKinerjaDosen penunjangKinerjaDosen,
			boolean update) throws Exception {

		JSONArray jsonArray = new JSONArray();

		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.author");
		jsonMetadata.put("value", penunjangKinerjaDosen.getPegawai().getNama());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.editor");
		jsonMetadata.put("value", penunjangKinerjaDosen.getPegawai().getNama());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", penunjangKinerjaDosen.getKeterangan());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.type");
		jsonMetadata.put("value", penunjangKinerjaDosen.getJenis());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", penunjangKinerjaDosen.getNama());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.subject");
		jsonMetadata.put("value", penunjangKinerjaDosen.getJenis());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.uri");
		jsonMetadata.put("value", penunjangKinerjaDosen.getBuktiDokumen());
		jsonArray.put(jsonMetadata);

		if (penunjangKinerjaDosen.getTanggalMulai() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(penunjangKinerjaDosen.getTanggalMulai()));
			jsonArray.put(jsonMetadata);
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		LampiranLain lampiranLain = LampiranLain.ambil(penunjangKinerjaDosen.getId(), LampiranLain.BUKTI_PENUGASAN);
		if (lampiranLain != null) {
			String uri = lampiranLain.createLinkUri(false);
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, penunjangKinerjaDosen,
				jsonPost.toString(), jsonArray.toString(), update, "items",
				"collections/" + getDspaceTipePenunjangKinerjaDosen(cookie, penunjangKinerjaDosen) + "/items",
				"items/{uuid}/metadata");

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"File " + LampiranLain.BUKTI_PENUGASAN);
		}

		lampiranLain = LampiranLain.ambil(penunjangKinerjaDosen.getId(), LampiranLain.BUKTI_DOKUMEN);

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"File " + LampiranLain.BUKTI_DOKUMEN);
		}

		StreamingHibernateUtil.getInstance().closeSession();

		return dspaceInformation;
	}

	class PenunjangKinerjaDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenunjangKinerjaDosen penunjangKinerjaDosen = (PenunjangKinerjaDosen) arg1;

			PenunjangKinerjaDosenAction.displayRow(arg0, penunjangKinerjaDosen, ases);

			// Kolom aksi rapi: semua tombol dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(penunjangKinerjaDosen);
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
											Common.refreshDelete(penunjangKinerjaDosen);
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
		init(new PenunjangKinerjaDosen());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PenunjangKinerjaDosen penunjangKinerjaDosen) {
		this.penunjangKinerjaDosen = penunjangKinerjaDosen;
		addWindow.setTitle("Pendataan " + jenis);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai (*)"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());
		pegawai.setAttribute("myValue", penunjangKinerjaDosen.getPegawai());
		pegawai.setAttribute("pegawai", penunjangKinerjaDosen.getPegawai());
		pegawai.setValue(
				penunjangKinerjaDosen.getPegawai() == null ? "" : penunjangKinerjaDosen.getPegawai().getNama());
		pegawai.setWidth("90%");

		if (pegawaiTerpilih != null) {
			pegawai.setAttribute("myValue", pegawaiTerpilih);
			pegawai.setAttribute("pegawai", pegawaiTerpilih);
			pegawai.setValue(pegawaiTerpilih.getNama());
			pegawai.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan " + jenis + " (*)"));
		row.appendChild(nama = new Textbox(penunjangKinerjaDosen.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bukti Penugasan " + jenis + " (*)"));
		row.appendChild(bukti = new Textbox(penunjangKinerjaDosen.getBukti()));
		bukti.setWidth("90%");
		bukti.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, penunjangKinerjaDosen.getId(), LampiranLain.BUKTI_PENUGASAN,
				LampiranLain.BUKTI_PENUGASAN, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						buktiPenugasan = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		Common.initKeterangan(rows,
				"*) Kompres atau zip dulu jika bukti penugasan lebih dari satu file, sehingga menjadi satu file yang Anda upload");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SKS Beban (*)"));
		row.appendChild(sks = new MyDoublebox(penunjangKinerjaDosen.getSks()));
		sks.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa / Lama Penugasan " + jenis + " (*)"));
		row.appendChild(masaPenugasan = new Textbox(penunjangKinerjaDosen.getMasaPenugasan()));
		masaPenugasan.setWidth("90%");

		Common.initKeterangan(rows, "Misal: 1 tahun, 6 bulan, 2 minggu, 5 hari, 8 jam");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kegiatan"));
		Hbox myHbox = new Hbox();
		myHbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Mulai ")));
		myHbox.appendChild(tanggalMulai = new MyDatebox(penunjangKinerjaDosen.getTanggalMulai()));
		myHbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		myHbox.appendChild(tanggalSampai = new MyDatebox(penunjangKinerjaDosen.getTanggalSampai()));
		row.appendChild(myHbox);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik (*)"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		if (penunjangKinerjaDosen.getTahunAkademik() != null) {
			Common.selectComboItem(tahunAkademik, penunjangKinerjaDosen.getTahunAkademik());
		}
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		semester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);

		Common.selectComboItem(semester, penunjangKinerjaDosen.getSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (*)"));
		row.appendChild(semester);
		semester.setReadonly(true);

		tanggalMulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tanggalMulai.getValue() != null) {
					Common.selectComboItem(tahunAkademik, Common.getCurrentTahunAkademik(tanggalMulai.getValue()));
					Common.selectComboItem(semester,
							Common.isNowSemensterGanjil(tanggalMulai.getValue()) ? Perkuliahan.GANJIL
									: Perkuliahan.GENAP);
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bukti Dokumen (*)"));
		row.appendChild(buktiDokumen = new Textbox(penunjangKinerjaDosen.getBuktiDokumen()));
		buktiDokumen.setWidth("90%");
		buktiDokumen.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, penunjangKinerjaDosen.getId(), LampiranLain.BUKTI_DOKUMEN,
				LampiranLain.BUKTI_DOKUMEN, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiranBuktiDokumen = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		Common.initKeterangan(rows,
				"*) Kompres atau zip dulu jika bukti dokumen lebih dari satu file, sehingga menjadi satu file yang Anda upload");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("URL Bukti Dokumen"));
		row.appendChild(linkDokumen = new Textbox(penunjangKinerjaDosen.getLinkDokumen()));
		linkDokumen.setWidth("90%");
		linkDokumen.setRows(2);

		Common.initKeterangan(rows, "Misal: http://www.dokumenku.com?id=3");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				penunjangKinerjaDosen.getKeterangan() == null ? "" : penunjangKinerjaDosen.getKeterangan()));
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
		if (pegawai.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Pegawai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Kegiatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (bukti.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Bukti Penugasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sks.getValue() == null) {
			MyMessageboxConfig.show("SKS Beban harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		
		if (masaPenugasan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Masa / Lama Penugasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (buktiDokumen.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Bukti Dokumen Penugasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (penunjangKinerjaDosen.getId() != null) {
			penunjangKinerjaDosen = (PenunjangKinerjaDosen) session.load(PenunjangKinerjaDosen.class,
					penunjangKinerjaDosen.getId());

		}

		penunjangKinerjaDosen.setSemester((String) semester.getSelectedItem().getValue());
		penunjangKinerjaDosen.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		penunjangKinerjaDosen.setNama(nama.getValue());
		penunjangKinerjaDosen.setBukti(bukti.getValue());
		penunjangKinerjaDosen.setSks(sks.getValue());
		penunjangKinerjaDosen.setMasaPenugasan(masaPenugasan.getValue());
		penunjangKinerjaDosen.setBuktiDokumen(buktiDokumen.getValue());
		penunjangKinerjaDosen.setLinkDokumen(linkDokumen.getValue());
		penunjangKinerjaDosen.setKeterangan(keterangan.getValue());
		penunjangKinerjaDosen.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		penunjangKinerjaDosen.setTanggalMulai(tanggalMulai.getValue());
		penunjangKinerjaDosen.setTanggalSampai(tanggalSampai.getValue());
		penunjangKinerjaDosen.setJenis(jenis);

		Common.refreshSaveOrUpdate(session, penunjangKinerjaDosen);

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (buktiPenugasan != null && buktiPenugasan.getId() != null) {
				session.refresh(buktiPenugasan);
				buktiPenugasan.setRef(penunjangKinerjaDosen.getId());

				session.getTransaction().begin();
				session.update(buktiPenugasan);
				session.getTransaction().commit();
			}

			if (lampiranBuktiDokumen != null && lampiranBuktiDokumen.getId() != null) {
				session.refresh(lampiranBuktiDokumen);
				lampiranBuktiDokumen.setRef(penunjangKinerjaDosen.getId());

				session.getTransaction().begin();
				session.update(lampiranBuktiDokumen);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Tbmuser tbmuser = Common.getCurrentUser();
		Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenunjangKinerjaDosen.class).createAlias("pegawai", "pegawai")
				.add(dosen == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("pegawai.dosen", dosen));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(Restrictions.or(Restrictions.isNull("jenis"), Restrictions.eq("jenis", jenis)))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchbukti.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("bukti", searchbukti.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchbuktidokumen.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("buktiDokumen", searchbuktidokumen.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchpegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("pegawai", searchpegawai.getAttribute("pegawai"))));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenunjangKinerjaDosen> penunjangKinerjaDosen = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penunjangKinerjaDosen);
		grid.setRowRenderer(new PenunjangKinerjaDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
