package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonOnSearchdefault;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import net.sf.jmimemagic.Magic;

public class SlipGajiPegawaiBulananAction extends GenericAutowireComposer implements CommonOnSearchdefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchcode;
	private Textbox searchnama;
	private Intbox searchtahun;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private MyToolbarbuttonConfig add;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private boolean edit;

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

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (searchtahun != null) { searchtahun.setValue(Calendar.getInstance().get(Calendar.YEAR)); }

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
		if (add != null) { add.setVisible(false); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		if (edit) {
			Common.appendKeToolbar(prosesPenggajian("Upload dan Download Slip Gaji", "/img/svg/payments.svg"), add, comp);
		}
		onSearchDefault(null);

	}

	public MyToolbarbuttonConfig prosesPenggajian(String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Dan Bulan Penggajian", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");
				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaran(tahunAkademik);

				final Combobox tahunAkademikSampai = new Combobox();
				Common.generateTahunAjaran(tahunAkademikSampai);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("20%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				final Combobox tahun;
				final Combobox bulan;
				tahun = new Combobox();
				bulan = new Combobox();

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
				row.appendChild(tahun);
				tahun.setWidth("90%");

				int tahunLoginSlipGaji = 20;
				try {
					tahunLoginSlipGaji = Integer
							.parseInt(Common.getKonfigurasi("tahun_slip_gaji", "20").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SlipGajiPegawaiBulananAction.java:194");
					// TODO: handle exception
				}

				for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
						- tahunLoginSlipGaji; i < ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + 1; i++) {
					MyComboitemConfig comboitem = new MyComboitemConfig();
					comboitem.setValue(i);
					comboitem.setLabel(i + "");
					tahun.appendChild(comboitem);
				}
				tahun.setReadonly(true);
				Common.selectComboItem(tahun, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Bulan"));
				row.appendChild(bulan);
				bulan.setWidth("90%");
				Common.createComboBulan(bulan);

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
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Download Slip Gaji", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event arg0) throws Exception {
								final Integer ta = (Integer) tahun.getSelectedItem().getValue();
								final Integer bln = ((Integer) bulan.getSelectedItem().getValue()) + 1;

								String filename = Sessions.getCurrent().getWebApp()
										.getRealPath(
												"/tmp/slip_gaji_bulan_" + bln + "_" + ta + "_"
														+ URLEncoder.encode(Common.datetimeFormat2s.get()
																.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
														+ ".zip");
								File file;
								(file = new File(filename)).createNewFile();
								List<File> filenames = new ArrayList<File>();

								List<Pegawai> pegawais = initCriteria(true).list();

								for (Pegawai pegawai : pegawais) {
									LampiranLain lampiranLain = LampiranLain.ambil(pegawai.getId(),
											"SLIP_GAJI_" + bln + "_" + ta);
									if (lampiranLain != null && lampiranLain.ambilFile() != null) {
										File fileDataTemp = lampiranLain.ambilFile();

										File fileData = new File(file.getParentFile().getAbsolutePath() + "/"
												+ pegawai.getId() + "_" + pegawai.getNama() + "_" + "SLIP_GAJI_" + bln
												+ "_" + ta + "_" + fileDataTemp.getName());

										FileUtils.copyFile(fileDataTemp, fileData);

										filenames.add(fileData);
									} else {
										File fileData = new File(file.getParentFile().getAbsolutePath() + "/"
												+ pegawai.getId() + "_" + pegawai.getNama() + "_" + "SLIP_GAJI_" + bln
												+ "_" + ta + ".txt");
										fileData.createNewFile();
										filenames.add(fileData);
									}
								}
								Common.createZip(filenames, file);

								Filedownload.save(file, "application/zip");
							}
						});

					}
				});
				save.setParent(toolbar);

				save = new MyToolbarbuttonConfig("Upload Slip Gaji (zip)", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.setUpload(Common.ukuranFileUpload(500000));
				save.addEventListener("onUpload", new EventListener() {

					private void ambilFile(File dir, List<File> filesD) {

						if (dir != null && dir.exists()) {
							File[] files = dir.listFiles(new FilenameFilter() {
								public boolean accept(File dir, String name) {
									return !name.toLowerCase().endsWith(".txt");
								}
							});

							try {
								for (File d : files) {
									filesD.add(d);
								}

							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SlipGajiPegawaiBulananAction.java:307");
								// TODO: handle exception
							}
							files = dir.listFiles(new FilenameFilter() {
								public boolean accept(File dir, String name) {
									return dir.isDirectory();
								}
							});

							try {
								if (files != null && files.length > 0) {

									for (File dira : files) {
										ambilFile(dira, filesD);
									}
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SlipGajiPegawaiBulananAction.java:323");
								// TODO: handle exception
							}
						}

					}

					@Override
					public void onEvent(Event event) throws Exception {

						UploadEvent uploadEvent = (UploadEvent) event;
						Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

						if (!media.getName().toLowerCase().endsWith(".zip")) {
							MyMessageboxConfig.show("File yang diupload harus berupa zip", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						File folder = CommonMedia.getMediaDirectory();
						if (!folder.exists()) {
							folder.mkdirs();
						}

						final File f = new File(folder.getAbsolutePath() + "/"
								+ URLEncoder.encode(ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_"
										+ uploadEvent.getMedia().getName(), "UTF-8"));

						f.createNewFile();
						FileOutputStream fileOutputStream = new FileOutputStream(f);
						try {
							IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
						} catch (Exception e) {
							try {
								IOUtils.write(media.getStringData(), fileOutputStream);
							} catch (Exception ee) {
								IOUtils.write(media.getByteData(), fileOutputStream);
							}
						}

						fileOutputStream.close();

						Common.createDefaultTimer(new EventListener() {

							@SuppressWarnings({ "unchecked", "deprecation" })
							@Override
							public void onEvent(Event arg0) throws Exception {
								Tbmuser tbmuser = Common.getCurrentUser();
								Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawaiD = tbmuser == null ? null : tbmuser.ambilPegawai();
								String olehId = Common.generateOlehId(tbmuser);

								File folder = CommonMedia.getMediaDirectory();
								if (!folder.exists()) {
									folder.mkdirs();
								}

								File folderZip = new File(
										folder.getAbsolutePath() + "/" + Common.getGeneratedBarCode());
								folderZip.mkdirs();

								Common.extractZip(f, folderZip.getAbsolutePath(), true);

								System.out.println("folderZip -> " + folderZip.getAbsolutePath());

								final Integer ta = (Integer) tahun.getSelectedItem().getValue();
								final Integer bln = ((Integer) bulan.getSelectedItem().getValue()) + 1;

								List<File> filesD = new ArrayList<File>();
								ambilFile(folderZip, filesD);

								System.out.println("filesD -> " + filesD.size());

								for (File f : filesD) {
									try {
										Long idPeg = Long.parseLong(f.getName().split("_")[0]);

										Pegawai pegawai = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(),
												idPeg);
										if (pegawai != null && pegawai.getId() != null) {

											LampiranLain lampiranLain = LampiranLain.ambil(pegawai.getId(),
													"SLIP_GAJI_" + bln + "_" + ta);

											if (lampiranLain == null) {
												lampiranLain = new LampiranLain();
											}

											String mimeType = Magic.getMagicMatch(f, false).getMimeType();

											lampiranLain.setRef(pegawai.getId());
											lampiranLain.setNama(f.getName());
											lampiranLain.setLink(null);
											lampiranLain.setKeterangan(mimeType);
											lampiranLain.setJenis("SLIP_GAJI_" + bln + "_" + ta);
											lampiranLain.setOlehId(olehId);
											lampiranLain.setOleh(tbmuser == null ? "external_update"
													: mahasiswa != null ? mahasiswa.getNama()
															: dosen != null ? dosen.getNama()
																	: pegawaiD != null ? pegawaiD.getNama()
																			: (tbmuser.getUserNama()));

											Session streamingSession = StreamingHibernateUtil.getInstance()
													.currentSession();
											try {

												streamingSession.getTransaction().begin();
												streamingSession.save(lampiranLain);
												streamingSession.getTransaction().commit();

												if (f != null && f.exists()) {
													lampiranLain.setFoto(new javax.sql.rowset.serial.SerialBlob(IOUtils
															.toByteArray(new FileInputStream(f.getAbsolutePath()))));
												}
												streamingSession.getTransaction().begin();
												streamingSession.update(lampiranLain);
												streamingSession.getTransaction().commit();

											} catch (Exception e) {
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												Common.tampilErrorJikaAdmin(e);
											}

											StreamingHibernateUtil.getInstance().closeSession();
										}
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
								}

								String filename = Sessions.getCurrent().getWebApp()
										.getRealPath(
												"/tmp/hasil_upload_slip_gaji_bulan_" + bln + "_" + ta + "_"
														+ URLEncoder.encode(Common.datetimeFormat2s.get()
																.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
														+ ".zip");
								File file;
								(file = new File(filename)).createNewFile();
								List<File> filenames = new ArrayList<File>();

								List<Pegawai> pegawais = initCriteria(true).list();

								for (Pegawai pegawai : pegawais) {
									LampiranLain lampiranLain = (LampiranLain) LampiranLain.ambil(pegawai.getId(),
											"SLIP_GAJI_" + bln + "_" + ta, LampiranLain.class, true);
									if (lampiranLain != null && lampiranLain.ambilFile() != null) {
										File fileDataTemp = lampiranLain.ambilFile();

										File fileData = new File(file.getParentFile().getAbsolutePath() + "/"
												+ pegawai.getId() + "_" + pegawai.getNama() + "_" + "SLIP_GAJI_" + bln
												+ "_" + ta + "_" + fileDataTemp.getName());

										FileUtils.copyFile(fileDataTemp, fileData);

										filenames.add(fileData);
									} else {
										File fileData = new File(file.getParentFile().getAbsolutePath() + "/"
												+ pegawai.getId() + "_" + pegawai.getNama() + "_" + "SLIP_GAJI_" + bln
												+ "_" + ta + ".txt");
										fileData.createNewFile();
										filenames.add(fileData);
									}
								}
								Common.createZip(filenames, file);

								Filedownload.save(file, "application/zip");

								onSearchDefault(null);
							}
						});

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		return toolbarbutton;
	}

	class PegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			Integer tahun = searchtahun.getValue();
			if (tahun == null) {
				tahun = Calendar.getInstance().get(Calendar.YEAR);
			}
			Pegawai pegawai = (Pegawai) arg1;

			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Pegawai.class, pegawai, pegawai.getNama())).setParent(arg0);
			new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(a);

			for (int i = 1; i <= 12; i++) {
				Vbox myvbox = new Vbox();
				myvbox.setParent(arg0);

				Hbox hbox = new Hbox();
				hbox.setParent(myvbox);
				LampiranLain.createDownloadUploadFileLain(hbox, pegawai.getId(), "SLIP_GAJI_" + i + "_" + tahun,
						"Bln " + i, true, null, null, false, true, false, edit);
			}
		}

	}

	public Criteria initCriteria(boolean order) {
		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pegawai.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(
						parent == null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"),
						Restrictions.in("satuanKerja", satuanKerjas)))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("code", searchcode.getValue(), MatchMode.ANYWHERE));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.initPaging(initCriteria(false), paging);

				List<Pegawai> pegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(pegawai);
				grid.setRowRenderer(new PegawaiRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

}
