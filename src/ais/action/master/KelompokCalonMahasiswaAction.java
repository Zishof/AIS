package ais.action.master;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Group;
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

import ais.action.master.helper.AmbilDataGelombangPendaftaranBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.pmb.KelompokCalonMahasiswaDetailAction;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.KelompokCalonMahasiswa;
import ais.database.model.KelompokParameterTambahanCalonMahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPaket;
import ais.database.model.JenisSeleksi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.kkn.PersyaratanKkn;
import ais.database.model.pkl.PersyaratanPkl;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextboxAngka;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KelompokCalonMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private AmbilDataGelombangPendaftaranBanbox searchgelombangPendaftaran;

	private Combobox gelombangPendaftaran;
	private Combobox statusAwalMahasiswa;
	private Combobox jenisSeleksiTarget;
	private Intbox kuota;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KelompokCalonMahasiswa kelompokCalonMahasiswa;
	private MyToolbarbuttonConfig add;
	private Rows rowsParameter;

	private East east;

	private String[] contents = new String[] { "id", "statusAwalMahasiswa", "noRegistrasi", "noUjian", "nama" };
	private MyCheckboxConfig aktifkanPenggunaanSkor;
	private Intbox skorMulai;
	private Intbox skorSampai;
	private Combobox tahunAjaran;

	private Tabpanel manajemenStatusAwalMahasiswa;

	public void onManajemenStatusAwalMahasiswa(Event event) {
		if (manajemenStatusAwalMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenStatusAwalMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/status_awal_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel afiliasiCalonMahasiswa;

	public void onManajemenAfiliasi(Event event) {
		if (afiliasiCalonMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(afiliasiCalonMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/afiliasi_calon_mahasiswa.zul");
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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchgelombangPendaftaran.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(BiodataCalonMahasiswa.class,
				new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {
						Session session = HibernateUtil.currentSession();
						Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.createAlias("gelombangPendaftaran", "gelombangPendaftaran");

						if (order)
							criteria.addOrder(Order.desc("id"));
						criteria.add((searchgelombangPendaftaran == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchgelombangPendaftaran.getAttribute("gelombangPendaftaran") == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("gelombangPendaftaran",
										searchgelombangPendaftaran.getAttribute("gelombangPendaftaran"))));
						return criteria;
					}
				}, "Download Data Kelompok Calon Mahasiswa", "/img/print.png", contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
				"Upload Data Kelompok Calon Mahasiswa" + Common.ukuranLabelFileUpload(), "/img/excel.png");
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final Label peringatan = new Label("");

							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
							Clients.showBusy(label.getValue());
							final Timer timer = new Timer(200);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {
										System.out.println("loading file " + file.getAbsolutePath());
										MyMessageboxConfig.show("Upload data berhasil dilakukan."
												+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
												new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														onSearchDefault(arg0);
													}
												});
										Clients.clearBusy();
										timer.detach();
									}

								}
							});
							timer.start();

							new Thread(new Runnable() {

								@Override
								public void run() {
									try {

									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												Session session = HibernateUtil.currentNativeSession();
												Long id = Common.getSheetContentAsLong(sheet, 0, i);
												BiodataCalonMahasiswa biodataCalonMahasiswa = id == null
														|| id.equals(-1L)
																? null
																: (BiodataCalonMahasiswa) session
																		.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																		.add(Restrictions.idEq(id)).uniqueResult();

												if (biodataCalonMahasiswa == null) {
													String noReg = Common.getSheetContentAsString(sheet, 2, i);
													if (noReg != null && !noReg.trim().isEmpty()) {
														biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
																.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																.setMaxResults(1).addOrder(Order.desc("id"))
																.add(Restrictions.eq("noRegistrasi", noReg))
																.uniqueResult();
													}
												}

												if (biodataCalonMahasiswa == null) {
													String noReg = Common.getSheetContentAsString(sheet, 2, i);
													if (noReg != null && !noReg.trim().isEmpty()) {
														biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
																.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																.setMaxResults(1).addOrder(Order.desc("id"))
																.add(Restrictions.eq("noRegistrasi", noReg))
																.uniqueResult();
													}
												}

												if (biodataCalonMahasiswa == null) {
													String noUjian = Common.getSheetContentAsString(sheet, 3, i);
													if (noUjian != null && !noUjian.trim().isEmpty()) {
														biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
																.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																.setMaxResults(1).addOrder(Order.desc("id"))
																.add(Restrictions.eq("noUjian", noUjian))
																.uniqueResult();
													}
												}

												// Tangkap ID biodata sebelum getSheetContentAsObject menutup session
												// (entitas menjadi DETACHED dan getGelombangPendaftaran() LAZY akan
												// melempar LazyInitializationException jika dipanggil setelah itu).
												final Long biodataId = biodataCalonMahasiswa == null ? null
														: biodataCalonMahasiswa.getId();

												StatusAwalMahasiswa statusAwalMahasiswaFromSheet = (StatusAwalMahasiswa) Common
														.getSheetContentAsObject(sheet, 1, i,
																StatusAwalMahasiswa.class);
												final Long statusAwalId = statusAwalMahasiswaFromSheet == null ? null
														: statusAwalMahasiswaFromSheet.getId();

												if (biodataId != null && statusAwalId != null) {

													// getSheetContentAsObject menutup session native → ambil ulang
													session = HibernateUtil.currentNativeSession();

													// Reload semua entitas di sesi baru — hindari detached proxy
													// yang menyebabkan LazyInitializationException.
													BiodataCalonMahasiswa biodata = (BiodataCalonMahasiswa) session
															.get(BiodataCalonMahasiswa.class, biodataId);
													StatusAwalMahasiswa statusAwal = (StatusAwalMahasiswa) session
															.get(StatusAwalMahasiswa.class, statusAwalId);

													if (biodata != null && statusAwal != null) {
														GelombangPendaftaran gp = biodata.getGelombangPendaftaran();

														KelompokCalonMahasiswa kelompokCalonMahasiswa = (KelompokCalonMahasiswa) session
																.createCriteria(KelompokCalonMahasiswa.class)
																.add(gp == null
																		? Restrictions.isNull("gelombangPendaftaran")
																		: Restrictions.eq("gelombangPendaftaran", gp))
																.add(Restrictions.eq("statusAwalMahasiswa", statusAwal))
																.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
														if (kelompokCalonMahasiswa == null) {
															kelompokCalonMahasiswa = new KelompokCalonMahasiswa();
															kelompokCalonMahasiswa.setGelombangPendaftaran(gp);
															kelompokCalonMahasiswa.setStatusAwalMahasiswa(statusAwal);
															kelompokCalonMahasiswa.setAktifkanPenggunaanSkor(true);
															kelompokCalonMahasiswa.setSkorMulai(1000);
															kelompokCalonMahasiswa.setSkorSampai(1000);
															session.getTransaction().begin();
															session.saveOrUpdate(kelompokCalonMahasiswa);
															session.getTransaction().commit();
														}

														biodata.setKelompokCalonMahasiswa(kelompokCalonMahasiswa);
														biodata.setStatusAwalMahasiswa(statusAwal);

														session.getTransaction().begin();
														session.saveOrUpdate(biodata);
														session.getTransaction().commit();

														label.setValue("Upload data \""
																+ biodata.getNoRegistrasi() + " - "
																+ biodata.getNama() + "\" ("
																+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
													}
												}

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
											HibernateUtil.closeSession();
										}
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/KelompokCalonMahasiswaAction.java:390");
									}

									label.setValue("");
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		Common.appendKeToolbar(upload, add, comp);
	}

	class KelompokCalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokCalonMahasiswa kelompokCalonMahasiswa = (KelompokCalonMahasiswa) arg1;

			(new KelompokCalonMahasiswaDetailAction(kelompokCalonMahasiswa)).setParent(arg0);

			String l = kelompokCalonMahasiswa.getGelombangPendaftaran().getNama() + "-"
					+ kelompokCalonMahasiswa.getGelombangPendaftaran().getTahunAkademik() + "-"
					+ kelompokCalonMahasiswa.getGelombangPendaftaran().getJenisSemester();
			RevisiHelper.createNewRevisi(KelompokCalonMahasiswa.class, kelompokCalonMahasiswa, l).setParent(arg0);
			new Label(kelompokCalonMahasiswa.getStatusAwalMahasiswa() == null ? ""
					: kelompokCalonMahasiswa.getStatusAwalMahasiswa().getNama()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			Session session = HibernateUtil.currentSession();
			int masukOtomatis = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNull("kelompokCalonMahasiswa"))
					.add(Restrictions.and(
							Restrictions.eq("gelombangPendaftaran", kelompokCalonMahasiswa.getGelombangPendaftaran()),
							Restrictions.eq("statusAwalMahasiswa", kelompokCalonMahasiswa.getStatusAwalMahasiswa())))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();

			int masukManual = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("kelompokCalonMahasiswa", kelompokCalonMahasiswa))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();

			/*
			 * PENYEBUT "Semua" = SELURUH calon di gelombang ini, termasuk yang sudah masuk
			 * kelompok mana pun.
			 *
			 * Sebelumnya di sini ikut dipasang Restrictions.isNull("kelompokCalonMahasiswa"),
			 * sehingga penyebutnya hanya menghitung calon yang BELUM terkelompok - padahal
			 * pembilangnya (masukOtomatis + masukManual) justru memasukkan anggota manual yang
			 * kelompoknya PASTI tidak null. Dua populasi yang berbeda dibandingkan, jadi
			 * persennya bisa melampaui 100% begitu sebuah kelompok punya anggota manual
			 * (mis. Otomatis 4 + Manual 112 dibagi 64 -> 181,25%).
			 */
			int masukSemuaGelombang = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("gelombangPendaftaran", kelompokCalonMahasiswa.getGelombangPendaftaran()))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();

			int total = masukOtomatis + masukManual;

			double persen = masukSemuaGelombang > 0 ? (total * 100.0 / masukSemuaGelombang) : 0.0;

			String kuotaHtml = "<table style='font-size:11px;border-collapse:collapse;line-height:1.4'>"
					+ "<tr>"
					+ "<td style='padding:1px 5px 1px 0;color:#777'>Kuota</td>"
					+ "<td style='padding:1px 10px 1px 2px;font-weight:600'>" + Common.numberFormat.get().format(kelompokCalonMahasiswa.getKuota()) + "</td>"
					+ "<td style='padding:1px 5px 1px 0;color:#777'>Otomatis</td>"
					+ "<td style='padding:1px 0 1px 2px;font-weight:600'>" + Common.numberFormat.get().format(masukOtomatis) + "</td>"
					+ "</tr><tr>"
					+ "<td style='padding:1px 5px 1px 0;color:#777'>Manual</td>"
					+ "<td style='padding:1px 10px 1px 2px;font-weight:600'>" + Common.numberFormat.get().format(masukManual) + "</td>"
					+ "<td style='padding:1px 5px 1px 0;color:#777'>Kelompok</td>"
					+ "<td style='padding:1px 0 1px 2px;font-weight:600'>" + Common.numberFormat.get().format(total) + "</td>"
					+ "</tr><tr>"
					+ "<td style='padding:1px 5px 1px 0;color:#777'>Semua</td>"
					+ "<td style='padding:1px 10px 1px 2px;font-weight:600'>" + Common.numberFormat.get().format(masukSemuaGelombang) + "</td>"
					+ "<td style='padding:1px 5px 1px 0;color:#777'>Persen</td>"
					+ "<td style='padding:1px 0 1px 2px;font-weight:600'>" + Common.numberFormat.get().format(persen) + "%</td>"
					+ "</tr></table>";
			org.zkoss.zul.Html kuotaHtmlComp = new org.zkoss.zul.Html();
			kuotaHtmlComp.setContent(kuotaHtml);
			kuotaHtmlComp.setParent(vbox);

			if (kelompokCalonMahasiswa.getAktifkanPenggunaanSkor()) {
				vbox = new Vbox();
				vbox.setParent(arg0);
				new MyLabelAgakKecil("Skor " + kelompokCalonMahasiswa.getAktifkanPenggunaanSkor()).setParent(vbox);
				new MyLabelAgakKecil("Mulai " + kelompokCalonMahasiswa.getSkorMulai()).setParent(vbox);
				new MyLabelAgakKecil("Sampai " + kelompokCalonMahasiswa.getSkorSampai()).setParent(vbox);
			} else {

				String[] s = StringUtils.split(kelompokCalonMahasiswa.getParameterTambahan(), "\n");
				vbox = new Vbox();
				vbox.setParent(arg0);
				for (String ss : s) {
					new MyLabelKecil(ss).setParent(vbox);
				}
			}

			// Kolom aksi rapi: semua tombol dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelompokCalonMahasiswa);
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
											Common.refreshDelete(kelompokCalonMahasiswa);
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
		init(new KelompokCalonMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static void validasiStatusAwalMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa,
			List<KelompokCalonMahasiswa> kelompokCalonMahasiswas) {
		if (biodataCalonMahasiswa.getKelompokCalonMahasiswa() != null) {
			return;
		}
		Integer skor = biodataCalonMahasiswa.getTotalSkor();
		Map<String, String> nilaiBio = new HashMap<String, String>();
		String[] spl = biodataCalonMahasiswa.getParameterTambahanInds().split("\n");
		for (String d : spl) {
			String[] value = d.split("<=>");
			String val = value.length > 1 ? value[1].trim() : "";
			if (!val.isEmpty()) {
				nilaiBio.put(value[0].trim(), val);
			}
		}

		Session session = HibernateUtil.currentSession();
		for (KelompokCalonMahasiswa kelompokCalonMahasiswa : kelompokCalonMahasiswas) {

			int masukOtomatis = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNull("kelompokCalonMahasiswa"))
					.add(Restrictions.and(
							Restrictions.eq("gelombangPendaftaran", kelompokCalonMahasiswa.getGelombangPendaftaran()),
							Restrictions.eq("statusAwalMahasiswa", kelompokCalonMahasiswa.getStatusAwalMahasiswa())))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (kelompokCalonMahasiswa.getKuota() > masukOtomatis) {

				if (kelompokCalonMahasiswa.getAktifkanPenggunaanSkor()) {
					if (skor >= kelompokCalonMahasiswa.getSkorMulai()
							&& skor <= kelompokCalonMahasiswa.getSkorSampai()) {
						biodataCalonMahasiswa.setStatusAwalMahasiswa(kelompokCalonMahasiswa.getStatusAwalMahasiswa());
						return;
					}
				} else {

					Map<String, String> nilais = new HashMap<String, String>();
					spl = kelompokCalonMahasiswa.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						String val = value.length > 1 ? value[1].trim() : "";
						if (!val.isEmpty()) {
							nilais.put(value[0].trim(), val);
						}
					}

					Boolean ada = false;
					Boolean cocokSemua = true;
					for (String key : nilaiBio.keySet()) {
						String nilai = nilaiBio.get(key).trim();
						for (String k : nilais.keySet()) {
							if (k.trim().equalsIgnoreCase(key)) {
								String nilaiK = nilais.get(k).trim();

								String[] ssval = StringUtils.split(nilaiK == null ? "" : nilaiK.trim(), "|");

								Boolean cocok = false;
								for (String v : ssval) {
									if (!v.trim().isEmpty() && !nilai.trim().isEmpty()
											&& v.trim().equalsIgnoreCase(nilai.trim())) {
										cocok = true;
										break;
									}
								}

								// Boolean cocok =
								// nilai.equalsIgnoreCase(nilaiK);
//								System.out.println("kelompokCalonMahasiswa=>" + kelompokCalonMahasiswa + ", key = "
//										+ key + ", nilai bio = " + nilai + ", nilai syarat = " + nilaiK + ", cocok = "
//										+ cocok);

								cocokSemua &= cocok;
								ada = true;
							}
						}
					}

					if (!ada) {
						cocokSemua = false;
					}

//					System.out.println(
//							"kelompokCalonMahasiswa=>" + kelompokCalonMahasiswa + ", cocokSemua = " + cocokSemua);
					if (cocokSemua) {
						biodataCalonMahasiswa
								.setStatusAwalMahasiswa(kelompokCalonMahasiswa == null ? ConstantValues.BARU
										: kelompokCalonMahasiswa.getStatusAwalMahasiswa());
						return;
					}
				}
			}
		}

		biodataCalonMahasiswa.setStatusAwalMahasiswa(ConstantValues.BARU);
	}

	@SuppressWarnings("unchecked")
	private void reloadDetail() throws Exception {
		Common.clear(east);
		if (gelombangPendaftaran.getSelectedItem() != null) {
			MyGrid gridParameter = new MyGrid();
			gridParameter.setWidth("100%");
			gridParameter.setParent(east);
			gridParameter.setWidth("100%");
			gridParameter.setHeight("100%");

			Columns columnsParameter = new Columns();
			columnsParameter.setParent(gridParameter);

			MyColumnConfig column = new MyColumnConfig("Aktifkan");
			column.setParent(columnsParameter);
			column.setWidth("60px");

			column = new MyColumnConfig("Parameter");
			column.setParent(columnsParameter);
			column.setWidth("30%");

			column = new MyColumnConfig("Nilai");
			column.setParent(columnsParameter);

			rowsParameter = new Rows();
			rowsParameter.setParent(gridParameter);

			GelombangPendaftaran gel = (GelombangPendaftaran) gelombangPendaftaran.getSelectedItem().getValue();
			Session session = HibernateUtil.currentSession();

			List<KelompokParameterTambahanCalonMahasiswa> kelompokParameterTambahanCalonMahasiswas = session
					.createCriteria(ParameterTambahanPaket.class)
					.add(Restrictions.or(Restrictions.eq("tampilDiSemuaGelombang", true),
							gel == null ? Restrictions.sqlRestriction("false")
									: Restrictions.ilike("gelombangs", ";" + gel.getId() + ";", MatchMode.ANYWHERE)))
					.createAlias("parameterTambahan", "parameterTambahan")
					.add(Restrictions.eq("parameterTambahan.aktif", true))
					.setProjection(Projections.groupProperty("kelompokParameterTambahanCalonMahasiswa")).list();
			Collections.sort(kelompokParameterTambahanCalonMahasiswas);
			for (KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa : kelompokParameterTambahanCalonMahasiswas) {

				Group group = new ais.ui.util.MyGroupConfig(kelompokParameterTambahanCalonMahasiswa.getNama());
				group.setParent(rowsParameter);

				List<ParameterTambahan> parameterTambahans = session.createCriteria(ParameterTambahanPaket.class)
						.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa",
								kelompokParameterTambahanCalonMahasiswa))
						.add(Restrictions.or(Restrictions.eq("tampilDiSemuaGelombang", true),
								gel == null ? Restrictions.sqlRestriction("false")
										: Restrictions.ilike("gelombangs", ";" + gel.getId() + ";",
												MatchMode.ANYWHERE)))
						.createAlias("parameterTambahan", "parameterTambahan")
						.add(Restrictions.eq("parameterTambahan.aktif", true))
						.setProjection(Projections.groupProperty("parameterTambahan")).list();
				Collections.sort(parameterTambahans);

				for (final ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanCalonMahasiswa.getId() + "->" + parameterTambahan.getId();

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setValign("top");row.setAttribute("parameterTambahan", parameterTambahan);
					row.setValign("top");row.setAttribute("kelompokParameterTambahanCalonMahasiswa",
							kelompokParameterTambahanCalonMahasiswa);
					row.setParent(rowsParameter);

					String val = "";
					Boolean par = false;
					String[] spl = kelompokCalonMahasiswa.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}

					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							par = true;
							break;
						}
					}

					final Component component;
					if (parameterTambahan.getTipeDataInputan().equals(PersyaratanKkn.TEXT)) {
						component = new Textbox(val);
						((Textbox) component).setWidth("90%");
						((Textbox) component).focus();
					} else if (parameterTambahan.getTipeDataInputan().equals(PersyaratanKkn.TANGGAL)) {
						Date nilai = null;
						try {
							nilai = val.trim().isEmpty() ? null : Common.dateFormat1.get().parse(val);
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						component = new MyDatebox(nilai);

						((MyDatebox) component).focus();
					} else if (parameterTambahan.getTipeDataInputan().equals(PersyaratanKkn.ANGKA)) {
						Double nilai = null;
						try {
							nilai = val.trim().isEmpty() ? null : Double.parseDouble(val);
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						component = new MyDoublebox(nilai);

					} else if (parameterTambahan.getTipeDataInputan().equals(PersyaratanKkn.TEXT_ANGKA)) {
						component = new MyTextboxAngka(val);
						((Textbox) component).setWidth("90%");
						((Textbox) component).focus();
					} else if (parameterTambahan.getTipeDataInputan().equals(PersyaratanKkn.PILIHAN_YA_TIDAK)) {
						Boolean nilai = null;
						try {
							nilai = val.trim().isEmpty() ? null : Boolean.parseBoolean(val);
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

						component = new Combobox();
						MyComboitemConfig comboitem = new MyComboitemConfig("Ya");
						comboitem.setValue(true);
						component.appendChild(comboitem);
						comboitem = new MyComboitemConfig("Tidak");
						comboitem.setValue(false);
						component.appendChild(comboitem);
						((Combobox) component).setReadonly(true);

						Common.selectComboItem(((Combobox) component), nilai);
					} else if (parameterTambahan.getTipeDataInputan().equals(PersyaratanKkn.PILIHAN_CUSTOM)) {
						component = new Hbox();
						String[] ss = StringUtils.split(parameterTambahan.getNilaiDataInputan(), ";");
						Arrays.sort(ss);
						String[] ssval = StringUtils.split(val == null ? "" : val.trim(), "|");

						for (String s : ss) {

							String[] kol = StringUtils.split(s, ":");
							// Guard: opsi tanpa ":" (mis. "Ya" bukan "Ya:10") membuat kol hanya berisi 1
							// elemen sehingga kol[1] melempar ArrayIndexOutOfBoundsException.
							String a = kol.length > 0 ? kol[0] : s;
							Integer skor = 0;
							try {
								if (kol.length > 1) {
									skor = Integer.parseInt(kol[1].trim());
								}
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

							MyCheckboxConfig checkbox = new MyCheckboxConfig(a + "(skor " + skor + ")");
							checkbox.setValue(s);
							component.appendChild(checkbox);

							for (String v : ssval) {
								if (!v.trim().isEmpty() && !s.trim().isEmpty() && v.trim().equalsIgnoreCase(s.trim())) {
									checkbox.setChecked(true);
									break;
								}
							}
						}
					} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.PILIHAN_BANYAK)) {
						component = new Vbox();
						String[] ss = StringUtils.split(parameterTambahan.getNilaiDataInputan(), ";");
						Arrays.sort(ss);
						for (String s : ss) {
							MyCheckboxConfig comboitem = new MyCheckboxConfig(s);
							comboitem.setValue(s);
							component.appendChild(comboitem);

							for (String g : val.split(";")) {
								if (g.trim().equalsIgnoreCase(s.trim())) {
									comboitem.setChecked(true);
								}
							}
						}

					} else {
						component = null;
					}

					final MyCheckboxConfig checkbox = new MyCheckboxConfig();
					EventListener eventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (component != null) {
								try {
									if (parameterTambahan.getTipeDataInputan().equals(PersyaratanPkl.TEXT)) {
										(((Textbox) component)).setDisabled(!checkbox.isChecked());
									} else if (parameterTambahan.getTipeDataInputan()
											.equals(PersyaratanPkl.PILIHAN_CUSTOM)) {
										Common.freeze(component, !checkbox.isChecked());
									} else if (parameterTambahan.getTipeDataInputan().equals(PersyaratanPkl.TANGGAL)) {
										(((MyDatebox) component)).setDisabled(!checkbox.isChecked());
									} else if (parameterTambahan.getTipeDataInputan().equals(PersyaratanPkl.ANGKA)) {
										(((MyDoublebox) component)).setDisabled(!checkbox.isChecked());
									} else if (parameterTambahan.getTipeDataInputan()
											.equals(PersyaratanPkl.TEXT_ANGKA)) {
										(((MyTextboxAngka) component)).setDisabled(!checkbox.isChecked());
									} else if (parameterTambahan.getTipeDataInputan()
											.equals(PersyaratanPkl.PILIHAN_YA_TIDAK)) {
										Combobox cb = (Combobox) component;
										if (cb.getSelectedItem() != null) {
											cb.getSelectedItem().setDisabled(!checkbox.isChecked());
										}
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KelompokCalonMahasiswaAction.java:848");
									// TODO: handle exception
								}
							}
						}
					};

					checkbox.setChecked(par);
					checkbox.setParent(row);
					row.setValign("top");row.setAttribute("checkbox", checkbox);
					checkbox.addEventListener("onClick", eventListener);

					row.appendChild(new ais.ui.util.MyLabelConfig(parameterTambahan.getLabelInputan()));
					if (component != null) {
						row.appendChild(component);
						row.setValign("top");row.setAttribute("component", component);
					}

					eventListener.onEvent(null);
				}
			}
		}

	}

	private void init(final KelompokCalonMahasiswa kelompokCalonMahasiswa) throws Exception {
		this.kelompokCalonMahasiswa = kelompokCalonMahasiswa;
		addWindow.setTitle(kelompokCalonMahasiswa.getId() == null ? "Tambah Kelompok Calon Mahasiswa" : "Ubah Kelompok Calon Mahasiswa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("75%");

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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		Common.generateTahunAjaran(tahunAjaran = new Combobox());
		row.appendChild(tahunAjaran);

		if (kelompokCalonMahasiswa.getGelombangPendaftaran() == null) {
			String tahunAkademikPenerimaanMahasiswaBaru = Common
					.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik())
					.getNilai();
			Common.selectComboItem(tahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);
		} else {
			Common.selectComboItem(tahunAjaran, kelompokCalonMahasiswa.getGelombangPendaftaran().getTahunAkademik());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran *"));
		row.appendChild(gelombangPendaftaran = new Combobox());
		gelombangPendaftaran.setWidth("90%");
		gelombangPendaftaran.setReadonly(true);
		gelombangPendaftaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadDetail();
			}
		});

		EventListener gelombangEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertCombo(gelombangPendaftaran, new String[] { "nama", "mulai", "sampai", "jenisSeleksi" },
						"tahunAkademik", GelombangPendaftaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								tahunAjaran.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tahunAkademik", tahunAjaran.getSelectedItem().getValue())));

				Common.selectComboItem(gelombangPendaftaran, kelompokCalonMahasiswa.getGelombangPendaftaran());
			}
		};

		gelombangEventListener.onEvent(null);
		tahunAjaran.addEventListener("onChange", gelombangEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal Mahasiswa *"));
		row.appendChild(statusAwalMahasiswa = new Combobox());
		Common.insertCombo(statusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusAwalMahasiswa, kelompokCalonMahasiswa.getStatusAwalMahasiswa());
		statusAwalMahasiswa.setWidth("90%");
		statusAwalMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Seleksi Target (opsional)"));
		row.appendChild(jenisSeleksiTarget = new Combobox());
		// PERBAIKAN celah "Gratis Pol jadi reguler": bila diisi, mahasiswa yang ditambahkan
		// MANUAL ke kelompok ini (lewat "Ambil Data Calon Mahasiswa Manual") akan otomatis
		// disesuaikan Jenis Seleksi-nya ke nilai ini juga -- karena Setting Biaya mencocokkan
		// tagihan berdasarkan Jenis Seleksi, BUKAN Kelompok. Kosongkan bila kelompok ini tidak
		// terkait beasiswa/Jenis Seleksi tertentu (mis. kelompok berbasis skor semata).
		Common.insertCombo(jenisSeleksiTarget, "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisSeleksiTarget, kelompokCalonMahasiswa.getJenisSeleksiTarget());
		jenisSeleksiTarget.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kuota"));
		row.appendChild(kuota = new Intbox(kelompokCalonMahasiswa.getKuota()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktifkan Penggunaan Skor"));
		row.appendChild(aktifkanPenggunaanSkor = new MyCheckboxConfig());
		aktifkanPenggunaanSkor.setChecked(kelompokCalonMahasiswa.getAktifkanPenggunaanSkor());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Skor Mulai"));
		row.appendChild(skorMulai = new Intbox(kelompokCalonMahasiswa.getSkorMulai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Skor Sampai"));
		row.appendChild(skorSampai = new Intbox(kelompokCalonMahasiswa.getSkorSampai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				kelompokCalonMahasiswa.getKeterangan() == null ? "" : kelompokCalonMahasiswa.getKeterangan()));
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

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				skorMulai.setDisabled(!aktifkanPenggunaanSkor.isChecked());
				skorSampai.setDisabled(!aktifkanPenggunaanSkor.isChecked());

				east.setWidth(aktifkanPenggunaanSkor.isChecked() ? "0%" : "75%");
				addWindow.setWidth(aktifkanPenggunaanSkor.isChecked() ? "300px" : "95%");
			}
		};

		aktifkanPenggunaanSkor.addEventListener("onClick", eventListener);
		eventListener.onEvent(null);
		reloadDetail();
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (gelombangPendaftaran.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Gelombang pendaftaran",
					"Kolom Gelombang pendaftaran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Gelombang pendaftaran.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (statusAwalMahasiswa.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Gelombang pendaftaran",
					"Kolom Gelombang pendaftaran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Gelombang pendaftaran.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokCalonMahasiswa.getId() != null) {
			kelompokCalonMahasiswa = (KelompokCalonMahasiswa) session.load(KelompokCalonMahasiswa.class,
					kelompokCalonMahasiswa.getId());

		}

		kelompokCalonMahasiswa
				.setGelombangPendaftaran((GelombangPendaftaran) gelombangPendaftaran.getSelectedItem().getValue());
		kelompokCalonMahasiswa
				.setStatusAwalMahasiswa((StatusAwalMahasiswa) statusAwalMahasiswa.getSelectedItem().getValue());
		kelompokCalonMahasiswa.setJenisSeleksiTarget((JenisSeleksi) (jenisSeleksiTarget.getSelectedItem() == null
				? null
				: jenisSeleksiTarget.getSelectedItem().getValue()));
		kelompokCalonMahasiswa.setKeterangan(keterangan.getValue());
		kelompokCalonMahasiswa.setSkorMulai(skorMulai.getValue());
		kelompokCalonMahasiswa.setSkorSampai(skorSampai.getValue());
		kelompokCalonMahasiswa.setAktifkanPenggunaanSkor(aktifkanPenggunaanSkor.isChecked());
		kelompokCalonMahasiswa.setKuota(kuota.getValue());

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		List<Component> parameterRows = rowsParameter.getChildren();
		for (Component c : parameterRows) {
			if (c instanceof Row) {
				Row row = (Row) c;
				if (row.getAttribute("checkbox") != null) {
					MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
					if (checkbox.isChecked()) {
						Component component = (Component) row.getAttribute("component");
						ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
						KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa = (KelompokParameterTambahanCalonMahasiswa) row
								.getAttribute("kelompokParameterTambahanCalonMahasiswa");
						if (parameterTambahan != null && kelompokParameterTambahanCalonMahasiswa != null) {
							String val = "";
							if (parameterTambahan.getTipeDataInputan().equals(PersyaratanPkl.TEXT)) {
								val = (((Textbox) component).getValue()).trim();
							} else if (parameterTambahan.getTipeDataInputan().equals(PersyaratanPkl.PILIHAN_CUSTOM)) {
								Hbox hbox = ((Hbox) component);
								for (Object o : hbox.getChildren()) {
									if (o instanceof MyCheckboxConfig) {
										if (((MyCheckboxConfig) o).isChecked()) {
											val += val.isEmpty() ? (((MyCheckboxConfig) o).getValue()).toString().trim()
													: "|" + (((MyCheckboxConfig) o).getValue()).toString().trim();
										}
									}
								}
							} else if (parameterTambahan.getTipeDataInputan().equals(PersyaratanPkl.TANGGAL)) {
								Date nilai = (((MyDatebox) component).getValue());
								val = nilai == null ? "" : Common.dateFormat1.get().format(nilai);
							} else if (parameterTambahan.getTipeDataInputan().equals(PersyaratanPkl.ANGKA)) {
								val = (((MyDoublebox) component).getValue()) + "";
							} else if (parameterTambahan.getTipeDataInputan().equals(PersyaratanPkl.TEXT_ANGKA)) {
								val = (((MyTextboxAngka) component).getValue()) + "";
							} else if (parameterTambahan.getTipeDataInputan().equals(PersyaratanPkl.PILIHAN_YA_TIDAK)) {
								val = ((Boolean) ((Combobox) component).getSelectedItem().getValue()) + "";
							}
							System.out.println("val => " + val);
							String s = kelompokParameterTambahanCalonMahasiswa.getNama() + "->"
									+ parameterTambahan.getLabelInputan() + "<=>" + val;
							parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;
							String sIds = kelompokParameterTambahanCalonMahasiswa.getId() + "->"
									+ parameterTambahan.getId() + "<=>" + val;
							parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
						}
					}
				}
			}
		}
		kelompokCalonMahasiswa.setParameterTambahan(parameterTambahanStr);
		kelompokCalonMahasiswa.setParameterTambahanInds(parameterTambahanInds);

		Common.refreshSaveOrUpdate(session, kelompokCalonMahasiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		GelombangPendaftaran gel = (GelombangPendaftaran) (searchgelombangPendaftaran.getAttribute("gelombangPendaftaran"));

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelompokCalonMahasiswa.class)
				.add(gel == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("gelombangPendaftaran", gel));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokCalonMahasiswa> kelompokCalonMahasiswas = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(kelompokCalonMahasiswas);

		grid.setRowRenderer(new KelompokCalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
