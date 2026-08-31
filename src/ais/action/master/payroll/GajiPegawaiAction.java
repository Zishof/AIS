package ais.action.master.payroll;

import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
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
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.employ.helper.MasaKerjaUtil;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.payroll.helper.ParameterTambahanGajiPegawaiListener;
import ais.action.master.payroll.util.ItemGajiPegawaiTreeModel;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bank;
import ais.database.model.Pegawai;
import ais.database.model.StatusPegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.Insentif;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.employ.Makan;
import ais.database.model.employ.Pendidikan;
import ais.database.model.employ.Transport;
import ais.database.model.file.LampiranLain;
import ais.database.model.kpi.PenilaianKpi;
import ais.database.model.payroll.Cabang;
import ais.database.model.payroll.Departemen;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.ItemGajiPegawai;
import ais.database.model.payroll.JenisGajiPegawai;
import ais.database.model.payroll.KelompokParameterTambahanGajiPegawai;
import ais.database.model.payroll.LevelJabatan;
import ais.database.model.payroll.PtkpPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk gaji pegawai. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Window addWindow}, {@code MyGrid grid},
 * {@code Paging paging}, {@code MyTextbox searchkode}, {@code MyTextbox searchnama}, {@code MyTextbox
 * searchjabatan}, {@code Combobox searchcabang}, {@code Combobox searchdepartemen}; inisialisasi/lifecycle
 * ({@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); mutasi data ({@code onResetTree()}, {@code onSave()}); operasi domain lain ({@code
 * onManajemenJenis()}, {@code onManajemenParameter()}, {@code informasiParameter()}, {@code informasiShift()},
 * {@code createMain()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class GajiPegawaiAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private MyTextbox searchnama;
	private MyTextbox searchjabatan;
	private Combobox searchcabang;
	private Combobox searchdepartemen;
	private Combobox searchlevelJabatan;
	private Combobox searchstatus;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private MyTextbox nama;
	private MyTextbox keterangan;
	private MyTextbox kode;
	private MyTextbox telp;
	private MyTextbox alamat;
	private Combobox pendidikan;
	private MyTextbox pangkat;
	private Combobox ptkpPegawai;
	private MyTextbox deskripsiPendidikan;
	private Combobox status;

//	private MyDatebox tanggalLahir;
//	private MyTextbox tempatLahir;
//	private Combobox agama;
//	private Combobox statusPerkawinan;
//	private Combobox jenisKelamin;
//	private Intbox jumlahAnak;
	private MyTextbox jamsostek;

	private Combobox cabang;
	private Combobox departemen;
	private Combobox levelJabatan;

	private Combobox bank;
	private MyTextbox norek;
	private MyTextbox ditransferAtasNama;

	private Combobox bank2;
	private MyTextbox norek2;
	private MyTextbox ditransferAtasNama2;

	private Combobox bank3;
	private MyTextbox norek3;
	private MyTextbox ditransferAtasNama3;

	private Combobox bank4;
	private MyTextbox norek4;
	private MyTextbox ditransferAtasNama4;

	private Combobox bank5;
	private MyTextbox norek5;
	private MyTextbox ditransferAtasNama5;

	private Combobox caraPembayaran;
	private Combobox caraPembayaran2;
	private Combobox caraPembayaran3;
	private Combobox caraPembayaran4;
	private Combobox caraPembayaran5;

	private boolean edit = false;

	private Pegawai pegawai;
	private MyToolbarbuttonConfig find;

	// private String tampilkanGaji = null;
	private East east;
	private MyDoublebox tunjanganKinerja;
	private MyDoublebox persenKpiDefault;

	private MyDoublebox jpDefault;
	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private Tabpanel manajemenJenis;

	public void onManajemenJenis(Event event) {
		if (manajemenJenis.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenJenis);
			MyInclude iframe = new MyInclude("/pages/master/payroll/jenis_gaji_pegawai.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel manajemenParameter;
	private Combobox jenisGajiPegawai;
	protected ParameterTambahanGajiPegawaiListener parameterTambahanListener;

	public void onManajemenParameter(Event event) {
		if (manajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/payroll/parameter_tambahan_gaji_pegawai.zul");
			iframe.setParent(window);
		}
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		// // tampilkanGaji = execution.getParameter("tampilkanGaji");
		// // System.out.println("tampilkanGaji = " + tampilkanGaji);
		//
		// add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		// add.setTooltiptext("Tambah");

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Common.insertComboDanSemua(searchcabang, "nama", Cabang.class);
		Common.insertComboDanSemua(searchdepartemen, "nama", Departemen.class);
		Common.insertComboDanSemua(searchlevelJabatan, "nama", LevelJabatan.class);
		Common.insertComboDanSemua(searchstatus, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));

		if (searchjabatan != null) { searchjabatan.setReadonly(true); }
		if (searchcabang != null) { searchcabang.setReadonly(true); }
		if (searchdepartemen != null) { searchdepartemen.setReadonly(true); }
		if (searchlevelJabatan != null) { searchlevelJabatan.setReadonly(true); }
		if (searchstatus != null) { searchstatus.setReadonly(true); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "nama", "tanggalMulaiPengalanKerja", "tanggalSampaiPengalanKerja",
				"tanggalmasukSemiTetap", "tanggalkeluarSemiTetap", "tanggalmasukHonorer", "tanggalkeluarHonorer",
				"tanggalmasuk", "tanggalkeluar", "tipeMasaKerja", "ptkpPegawai", "formatItemGaji", "nilaiGaji",
				"caraPembayaran", "bank", "norek", "ditransferAtasNama", "caraPembayaran2", "bank2", "norek2",
				"ditransferAtasNama2", "caraPembayaran3", "bank3", "norek3", "ditransferAtasNama3", "caraPembayaran4",
				"bank4", "norek4", "ditransferAtasNama4", "caraPembayaran5", "bank5", "norek5", "ditransferAtasNama5",
				"cabang", "departemen", "levelJabatan", "jpDefault" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		if (cetakToolbarbutton != null) { cetakToolbarbutton.setLabel("Download Format Penggajian"); }
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Pegawai.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] data = (Object[]) arg0.getData();
				Pegawai detail = (Pegawai) data[0];
				Session session = (Session) data[1];
				@SuppressWarnings("rawtypes")
				Map datum = (Map) data[2];
				try {
					String formatItemGajiD = (String) datum.get("formatItemGaji");
					Object jpDefault = datum.get("jpDefault");

					FormatItemGaji formatItemGaji = (FormatItemGaji) Common.getContentAsObject(formatItemGajiD,
							FormatItemGaji.class, null);

					System.out.println("formatItemGaji -> " + formatItemGaji + " jpDefault -> " + jpDefault);

					if (jpDefault != null) {
						try {
							Double j = Double.parseDouble(jpDefault.toString());
							if (j != null && j.intValue() != detail.getJpDefault().intValue()) {
								detail.setJpDefault(j);
								Common.refreshUpdate(detail);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/GajiPegawaiAction.java:293");
							// TODO: handle exception
						}
					}

					if (formatItemGaji != null && formatItemGaji.getId() != null) {

						detail.setFormatItemGaji(formatItemGaji);

						ItemGajiPegawai itemGajiPegawai = (ItemGajiPegawai) session
								.createCriteria(ItemGajiPegawai.class)
								.add(Restrictions.eq("formatItemGaji", formatItemGaji))
								.add(Restrictions.isNull("parent")).addOrder(Order.desc("nomorUrut")).setMaxResults(1)
								.uniqueResult();

						if (itemGajiPegawai != null) {
							ItemGajiPegawaiTreeModel itemGajiPegawaiTreeModel = new ItemGajiPegawaiTreeModel(true,
									formatItemGaji, detail, null);
							Double hasil = itemGajiPegawaiTreeModel.hitungItemGajiPegawai(itemGajiPegawai.getKode(),
									itemGajiPegawai.getDefaultFormula(), ais.ui.util.WaktuUtil.getDate(),
									ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1,
									ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR), null, null);

							if (itemGajiPegawai != null && itemGajiPegawai.getItemGaji() != null
									&& itemGajiPegawai.getItemGaji().getJadikan0JikaMinus() && hasil < 0.0) {
								hasil = 0.0;
							}
							detail.setNilaiGaji(hasil);
						}

					}

				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

		}, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

			}

		}, contents);
		if (upload != null) { upload.setLabel("Upload Format Penggajian"); }
		if (upload != null) { upload.setVisible(edit); }
		Common.appendKeToolbar(upload, find, comp);

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	public void onResetTree(Event event) throws Exception {
		MyMessageboxConfig.show(
				"Apakah Bapak/Ibu yakin ingin me-reset gaji seluruh pegawai sesuai kriteria pencarian saat ini? Tindakan ini akan mengganti data gaji pegawai yang terkait dan tidak dapat dikembalikan. Tekan OK untuk melanjutkan, atau Batal untuk membatalkan.",
				"Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {

							final Label label = Common.displayLoadBar(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});

							new Thread(new Runnable() {

								@SuppressWarnings("unchecked")
								@Override
								public void run() {
									try {

									List<Pegawai> pegawais = ConstantValues.simpleList(
											initCriteria(true).add(Restrictions.isNotNull("formatItemGaji")),
											Pegawai.class);

									int size = pegawais.size();
									int index = 0;
									for (Pegawai pegawai : pegawais) {
										index++;
										label.setValue("Memproses data rencana gaji " + pegawai.getNama() + " ("
												+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

										Double totalHasil = 0.0;
										for (FormatItemGaji formatItemGaji : pegawai.ambilFormatItemGajis()) {
											ItemGajiPegawaiTreeModel itemGajiPegawaiTreeModel = new ItemGajiPegawaiTreeModel(
													true, formatItemGaji, pegawai, null);
											itemGajiPegawaiTreeModel.reset();

											try {
												Session session = HibernateUtil.currentNativeSession();
												ItemGajiPegawai itemGajiPegawai = (ItemGajiPegawai) ConstantValues
														.simpleObject(session.createCriteria(ItemGajiPegawai.class)
																.add(Restrictions.eq("formatItemGaji", formatItemGaji))
																.add(Restrictions.isNull("parent"))
																.addOrder(Order.desc("nomorUrut")).setMaxResults(1),
																ItemGajiPegawai.class);

												if (itemGajiPegawai != null) {

													Double hasil = itemGajiPegawaiTreeModel.hitungItemGajiPegawai(
															itemGajiPegawai.getKode(),
															itemGajiPegawai.getDefaultFormula(),
															ais.ui.util.WaktuUtil.getDate(),
															ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1,
															ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR),
															null, null);

													if (itemGajiPegawai != null && itemGajiPegawai.getItemGaji() != null
															&& itemGajiPegawai.getItemGaji().getJadikan0JikaMinus()
															&& hasil < 0.0) {
														hasil = 0.0;
													}
													totalHasil += hasil;
												}

												// session.disconnect();
												if (session.isOpen()) {session.disconnect();session.close();}
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/GajiPegawaiAction.java:423");
												// TODO: handle exception
											}
											HibernateUtil.closeSession();
										}

										Session session = HibernateUtil.currentNativeSession();
										try {
											pegawai.setNilaiGaji(totalHasil);
											session.getTransaction().begin();
											Common.refreshUpdate(session, pegawai);
											session.getTransaction().commit();
											// session.disconnect();
											if (session.isOpen()) {session.disconnect();session.close();}
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/GajiPegawaiAction.java:437");
											// TODO: handle exception
										}
										HibernateUtil.closeSession();
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

	/**
	 * Renderer lokal untuk layar/komponen {@link GajiPegawaiAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link GajiPegawaiAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code DateTimeFormatter formatter}, {@code
	 * Collection user}; operasi lokal: {@code render}(). Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see GajiPegawaiAction
	 */
	class PegawaiRenderer extends ais.ui.util.MyRowRenderer {

		private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		@SuppressWarnings("rawtypes")
		private Collection user = ConstantValues.ambilBerdasarClass(Tbmuser.class).values();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pegawai pegawai = (Pegawai) arg1;

			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);
			Vbox vbox = RevisiHelper.createNewRevisi(Pegawai.class, pegawai,
					pegawai.getDosen() == null ? pegawai.getNama() : pegawai.getDosen().getNama());
			vbox.setParent(arg0);

			if (pegawai.getGuru() != null) {
				new MyLabelAgakKecil("Guru").setParent(vbox);
			} else if (pegawai.getDosen() != null) {
				new MyLabelAgakKecil("Dosen").setParent(vbox);
			} else if (pegawai != null) {

				for (Object o : user) {
					Tbmuser tbmuser = (Tbmuser) o;
					if (tbmuser.getAktif() && tbmuser.hakAkses() != null && tbmuser.ambilPegawai() != null
							&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
						if (tbmuser.getAktif()) {
							new MyLabelAgakKecil(tbmuser.hakAkses().getRoleName()).setParent(vbox);
						}
					}
				}

			}

			if (pegawai.getStatusKepegawaian() != null) {
				new MyLabelAgakKecil(pegawai.getStatusKepegawaian().getNama()).setParent(vbox);
			}

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(vbox);
			new Label(pegawai.getCode() == null ? "" : pegawai.getCode()).setParent(vbox);

			vbox = new Vbox();
			vbox.setParent(arg0);

			if (pegawai.getTanggalMulaiPengalanKerja() != null) {

				String ActualDate = Common.databaseDateFormat.get().format(pegawai.getTanggalMulaiPengalanKerja());
				java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
				java.time.LocalDate currentdate = pegawai.getTanggalSampaiPengalanKerja() == null
						? java.time.LocalDate.now()
						: java.time.LocalDate
								.parse(Common.databaseDateFormat.get().format(pegawai.getTanggalSampaiPengalanKerja()));
				Period period = Period.between(dt, currentdate);

				new Label("Pengalaman Kerja : " + period.getYears() + " thn, " + period.getMonths() + " bln, "
						+ period.getDays() + " hr").setParent(vbox);
			}
			if (pegawai.getTanggalmasukHonorer() != null) {

				String ActualDate = Common.databaseDateFormat.get().format(pegawai.getTanggalmasukHonorer());
				java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
				java.time.LocalDate currentdate = pegawai.getTanggalkeluarHonorer() == null ? java.time.LocalDate.now()
						: java.time.LocalDate
								.parse(Common.databaseDateFormat.get().format(pegawai.getTanggalkeluarHonorer()));
				Period period = Period.between(dt, currentdate);

				new Label("Honor : " + period.getYears() + " thn, " + period.getMonths() + " bln, " + period.getDays()
						+ " hr").setParent(vbox);
			}
			if (pegawai.getTanggalmasukSemiTetap() != null) {

				String ActualDate = Common.databaseDateFormat.get().format(pegawai.getTanggalmasukSemiTetap());
				java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
				java.time.LocalDate currentdate = pegawai.getTanggalkeluarSemiTetap() == null
						? java.time.LocalDate.now()
						: java.time.LocalDate
								.parse(Common.databaseDateFormat.get().format(pegawai.getTanggalkeluarSemiTetap()));
				Period period = Period.between(dt, currentdate);

				new Label("Semi Tetap : " + period.getYears() + " thn, " + period.getMonths() + " bln, "
						+ period.getDays() + " hr").setParent(vbox);
			}
			if (pegawai.getTanggalmasuk() != null) {

				String ActualDate = Common.databaseDateFormat.get().format(pegawai.getTanggalmasuk());
				java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
				java.time.LocalDate currentdate = pegawai.getTanggalkeluar() == null ? java.time.LocalDate.now()
						: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(pegawai.getTanggalkeluar()));
				Period period = Period.between(dt, currentdate);

				new Label("Tetap : " + period.getYears() + " thn, " + period.getMonths() + " bln, " + period.getDays()
						+ " hr").setParent(vbox);
			}

			Period period = MasaKerjaUtil.masaKerja(pegawai);
			new Label("Masa kerja " + period.getYears() + " tahun " + period.getMonths() + " bulan " + period.getDays()
					+ " hari").setParent(vbox);

			new Label(pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()).setParent(arg0);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			vbox2.appendChild(new Label(pegawai.getBank() == null ? "" : pegawai.getBank().getNama()));
			vbox2.appendChild(new Label(pegawai.getDitransferAtasNama()));
			vbox2.appendChild(new Label(pegawai.getNorek()));

			if (pegawai.getBank2() != null) {
				vbox2.appendChild(new Label(pegawai.getBank2() == null ? "" : pegawai.getBank2().getNama()));
				vbox2.appendChild(new Label(pegawai.getDitransferAtasNama2()));
				vbox2.appendChild(new Label(pegawai.getNorek2()));
			}

			if (pegawai.getBank3() != null) {
				vbox2.appendChild(new Label(pegawai.getBank3() == null ? "" : pegawai.getBank3().getNama()));
				vbox2.appendChild(new Label(pegawai.getDitransferAtasNama3()));
				vbox2.appendChild(new Label(pegawai.getNorek3()));
			}
			
			if (pegawai.getBank4() != null) {
				vbox2.appendChild(new Label(pegawai.getBank4() == null ? "" : pegawai.getBank4().getNama()));
				vbox2.appendChild(new Label(pegawai.getDitransferAtasNama4()));
				vbox2.appendChild(new Label(pegawai.getNorek4()));
			}
			
			if (pegawai.getBank5() != null) {
				vbox2.appendChild(new Label(pegawai.getBank5() == null ? "" : pegawai.getBank5().getNama()));
				vbox2.appendChild(new Label(pegawai.getDitransferAtasNama5()));
				vbox2.appendChild(new Label(pegawai.getNorek5()));
			}

			vbox2 = new Vbox();
			vbox2.setParent(arg0);
			new Label(pegawai.getFormatItemGaji() == null ? "" : pegawai.getFormatItemGaji().getNama())
					.setParent(vbox2);
			new Label(pegawai.getFormatItemGaji2() == null ? "" : pegawai.getFormatItemGaji2().getNama())
					.setParent(vbox2);
			new Label(pegawai.getFormatItemGaji3() == null ? "" : pegawai.getFormatItemGaji3().getNama())
					.setParent(vbox2);
			new Label(pegawai.getFormatItemGaji4() == null ? "" : pegawai.getFormatItemGaji4().getNama())
					.setParent(vbox2);
			new Label(pegawai.getFormatItemGaji5() == null ? "" : pegawai.getFormatItemGaji5().getNama())
					.setParent(vbox2);

			new Label(Common.numberFormat.get().format(pegawai.getNilaiGaji())).setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			Toolbarbutton button = new MyToolbarbuttonConfig("Struktur Gaji", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pegawai);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	@SuppressWarnings("deprecation")
	public void informasiParameter(Component component, final Pegawai pegawai) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(component);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Jenis Parameter Gaji"));
		row.appendChild(jenisGajiPegawai = new Combobox());
		jenisGajiPegawai.setWidth("90%");
		jenisGajiPegawai.setReadonly(true);

		Common.insertCombo(jenisGajiPegawai, new String[] { "nama", "kode" }, "keterangan", JenisGajiPegawai.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisGajiPegawai, pegawai.getJenisGajiPegawai());

		MyFormRow rowLampiran = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
		rowLampiran.setParent(rows);

		final Grid gridLampiran = new Grid();
		gridLampiran.setSclass("fgrid");
		gridLampiran.setParent(rowLampiran);

		Columns columns = new Columns();
		columns.setParent(gridLampiran);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		final Rows rowsLampiran = new Rows();
		rowsLampiran.setParent(gridLampiran);

		EventListener eventListenerJenisGajiPegawai = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				JenisGajiPegawai j = (JenisGajiPegawai) (jenisGajiPegawai.getSelectedItem() == null ? null
						: jenisGajiPegawai.getSelectedItem().getValue());

				if (j != null) {
					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();
					HibernateUtil.currentSession().refresh(j);

					Set<KelompokParameterTambahanGajiPegawai> kelompokParameterTambahanGajiPegawais = new TreeSet<KelompokParameterTambahanGajiPegawai>();
					for (KelompokParameterTambahanGajiPegawai kelompokParameterTambahanGajiPegawai : j
							.getKelompokParameterTambahanGajiPegawais()) {
						kelompokParameterTambahanGajiPegawais.add(kelompokParameterTambahanGajiPegawai);
					}

					parameterTambahanListener = new ParameterTambahanGajiPegawaiListener(pegawai,
							kelompokParameterTambahanGajiPegawais, parameterRows, lampiranLains, rowsLampiran);

					parameterTambahanListener.onEvent(null);
				}
			}

		};

		jenisGajiPegawai.addEventListener("onChange", eventListenerJenisGajiPegawai);

		eventListenerJenisGajiPegawai.onEvent(null);
	}

	public void informasiShift(Component component, final Pegawai pegawai) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(component);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		EventListener gajiEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(east);
				MyInclude include = new MyInclude(
						"/pages/master/payroll/item_gaji_pegawai.zul?pegawai=" + pegawai.getId());
				east.appendChild(include);
			}
		};

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Nomor Jamsostek"));
		row.appendChild(jamsostek = new MyTextbox(pegawai.getJamsostek()));
		jamsostek.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Nama Cabang"));
		row.appendChild(cabang = new Combobox());
		Common.insertCombo(cabang, "nama", Cabang.class);
		Common.selectComboItem(cabang, pegawai.getCabang());
		cabang.setWidth("90%");
		cabang.addEventListener("onChange", gajiEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Nama Departemen"));
		row.appendChild(departemen = new Combobox());
		Common.insertCombo(departemen, "nama", Departemen.class);
		Common.selectComboItem(departemen, pegawai.getDepartemen());
		departemen.setWidth("90%");
		departemen.addEventListener("onChange", gajiEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Jabatan"));
		row.appendChild(levelJabatan = new Combobox());
		Common.insertCombo(levelJabatan, "nama", LevelJabatan.class);
		Common.selectComboItem(levelJabatan, pegawai.getLevelJabatan());
		levelJabatan.setWidth("90%");
		levelJabatan.addEventListener("onChange", gajiEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Cara Pembayaran Utama"));
		row.appendChild(caraPembayaran = new Combobox());
		Comboitem comboitem = new Comboitem(Pegawai.CARA_BAYAR_DITRASFER);
		comboitem.setValue(Pegawai.CARA_BAYAR_DITRASFER);
		caraPembayaran.appendChild(comboitem);
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_TUNAI);
		comboitem.setValue(Pegawai.CARA_BAYAR_TUNAI);
		caraPembayaran.appendChild(comboitem);
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_LAINNYA);
		comboitem.setValue(Pegawai.CARA_BAYAR_LAINNYA);
		caraPembayaran.appendChild(comboitem);
		Common.selectComboItem(caraPembayaran, pegawai.getCaraPembayaran());
		caraPembayaran.setWidth("90%");
		caraPembayaran.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Ditransfer ke Bank Utama"));
		row.appendChild(bank = new Combobox());
		Common.insertComboDanSemua(bank, new String[] { "nama" }, "keterangan", Bank.class, "=Pilih Bank=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(true, bank, pegawai.getBank());
		bank.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("No. Rekening Utama"));
		row.appendChild(norek = new MyTextbox(pegawai.getNorek()));
		norek.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Ditransfer atas nama rekening utama"));
		row.appendChild(ditransferAtasNama = new MyTextbox(pegawai.getDitransferAtasNama()));
		ditransferAtasNama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Cara Pembayaran ke-2"));
		row.appendChild(caraPembayaran2 = new Combobox());
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_DITRASFER);
		comboitem.setValue(Pegawai.CARA_BAYAR_DITRASFER);
		caraPembayaran2.appendChild(comboitem);
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_TUNAI);
		comboitem.setValue(Pegawai.CARA_BAYAR_TUNAI);
		caraPembayaran2.appendChild(comboitem);
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_LAINNYA);
		comboitem.setValue(Pegawai.CARA_BAYAR_LAINNYA);
		caraPembayaran2.appendChild(comboitem);
		Common.selectComboItem(caraPembayaran2, pegawai.getCaraPembayaran2());
		caraPembayaran2.setWidth("90%");
		caraPembayaran2.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Ditransfer ke Bank ke-2"));
		row.appendChild(bank2 = new Combobox());
		Common.insertComboDanSemua(bank2, new String[] { "nama" }, "keterangan", Bank.class, "=Pilih Bank=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(true, bank2, pegawai.getBank2());
		bank2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("No. Rekening ke-2"));
		row.appendChild(norek2 = new MyTextbox(pegawai.getNorek2()));
		norek2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Ditransfer atas nama rekening ke-2"));
		row.appendChild(ditransferAtasNama2 = new MyTextbox(pegawai.getDitransferAtasNama2()));
		ditransferAtasNama2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Cara Pembayaran ke-3"));
		row.appendChild(caraPembayaran3 = new Combobox());
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_DITRASFER);
		comboitem.setValue(Pegawai.CARA_BAYAR_DITRASFER);
		caraPembayaran3.appendChild(comboitem);
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_TUNAI);
		comboitem.setValue(Pegawai.CARA_BAYAR_TUNAI);
		caraPembayaran3.appendChild(comboitem);
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_LAINNYA);
		comboitem.setValue(Pegawai.CARA_BAYAR_LAINNYA);
		caraPembayaran3.appendChild(comboitem);
		Common.selectComboItem(caraPembayaran3, pegawai.getCaraPembayaran3());
		caraPembayaran3.setWidth("90%");
		caraPembayaran3.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Ditransfer ke Bank ke-3"));
		row.appendChild(bank3 = new Combobox());
		Common.insertComboDanSemua(bank3, new String[] { "nama" }, "keterangan", Bank.class, "=Pilih Bank=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(true, bank3, pegawai.getBank3());
		bank3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("No. Rekening ke-3"));
		row.appendChild(norek3 = new MyTextbox(pegawai.getNorek3()));
		norek3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Ditransfer atas nama rekening ke-3"));
		row.appendChild(ditransferAtasNama3 = new MyTextbox(pegawai.getDitransferAtasNama3()));
		ditransferAtasNama3.setWidth("90%");
		
		
		
		
		
		
		
		
		
		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Cara Pembayaran ke-4"));
		row.appendChild(caraPembayaran4 = new Combobox());
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_DITRASFER);
		comboitem.setValue(Pegawai.CARA_BAYAR_DITRASFER);
		caraPembayaran4.appendChild(comboitem);
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_TUNAI);
		comboitem.setValue(Pegawai.CARA_BAYAR_TUNAI);
		caraPembayaran4.appendChild(comboitem);
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_LAINNYA);
		comboitem.setValue(Pegawai.CARA_BAYAR_LAINNYA);
		caraPembayaran4.appendChild(comboitem);
		Common.selectComboItem(caraPembayaran4, pegawai.getCaraPembayaran4());
		caraPembayaran4.setWidth("90%");
		caraPembayaran4.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Ditransfer ke Bank ke-4"));
		row.appendChild(bank4 = new Combobox());
		Common.insertComboDanSemua(bank4, new String[] { "nama" }, "keterangan", Bank.class, "=Pilih Bank=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(true, bank4, pegawai.getBank4());
		bank4.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("No. Rekening ke-4"));
		row.appendChild(norek4 = new MyTextbox(pegawai.getNorek4()));
		norek4.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Ditransfer atas nama rekening ke-4"));
		row.appendChild(ditransferAtasNama4 = new MyTextbox(pegawai.getDitransferAtasNama4()));
		ditransferAtasNama4.setWidth("90%");
		
		
		
		
		
		
		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Cara Pembayaran ke-5"));
		row.appendChild(caraPembayaran5 = new Combobox());
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_DITRASFER);
		comboitem.setValue(Pegawai.CARA_BAYAR_DITRASFER);
		caraPembayaran5.appendChild(comboitem);
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_TUNAI);
		comboitem.setValue(Pegawai.CARA_BAYAR_TUNAI);
		caraPembayaran5.appendChild(comboitem);
		comboitem = new Comboitem(Pegawai.CARA_BAYAR_LAINNYA);
		comboitem.setValue(Pegawai.CARA_BAYAR_LAINNYA);
		caraPembayaran5.appendChild(comboitem);
		Common.selectComboItem(caraPembayaran5, pegawai.getCaraPembayaran5());
		caraPembayaran5.setWidth("90%");
		caraPembayaran5.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Ditransfer ke Bank ke-5"));
		row.appendChild(bank5 = new Combobox());
		Common.insertComboDanSemua(bank5, new String[] { "nama" }, "keterangan", Bank.class, "=Pilih Bank=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(true, bank5, pegawai.getBank5());
		bank5.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("No. Rekening ke-5"));
		row.appendChild(norek5 = new MyTextbox(pegawai.getNorek5()));
		norek5.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Ditransfer atas nama rekening ke-5"));
		row.appendChild(ditransferAtasNama5 = new MyTextbox(pegawai.getDitransferAtasNama5()));
		ditransferAtasNama5.setWidth("90%");
		
		

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String cp = (String) (caraPembayaran.getSelectedItem() == null ? ""
						: caraPembayaran.getSelectedItem().getValue());
				bank.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				norek.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				ditransferAtasNama.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));

				cp = (String) (caraPembayaran2.getSelectedItem() == null ? ""
						: caraPembayaran2.getSelectedItem().getValue());
				bank2.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				norek2.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				ditransferAtasNama2.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));

				cp = (String) (caraPembayaran3.getSelectedItem() == null ? ""
						: caraPembayaran3.getSelectedItem().getValue());
				bank3.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				norek3.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				ditransferAtasNama3.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				
				
				
				cp = (String) (caraPembayaran4.getSelectedItem() == null ? ""
						: caraPembayaran4.getSelectedItem().getValue());
				bank4.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				norek4.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				ditransferAtasNama4.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				
				
				cp = (String) (caraPembayaran5.getSelectedItem() == null ? ""
						: caraPembayaran5.getSelectedItem().getValue());
				bank5.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				norek5.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				ditransferAtasNama5.setDisabled(!cp.equals(Pegawai.CARA_BAYAR_DITRASFER));
				
			}
		};
		caraPembayaran.addEventListener("onChange", eventListener);
		caraPembayaran2.addEventListener("onChange", eventListener);
		caraPembayaran3.addEventListener("onChange", eventListener);
		caraPembayaran4.addEventListener("onChange", eventListener);
		caraPembayaran5.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

	}

	public void createMain(Component component, Pegawai pegawai) throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(component);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Kode Pegawai"));
		row.appendChild(kode = new MyTextbox(pegawai.getKode() == null ? "" : pegawai.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Nama Pegawai"));
		row.appendChild(nama = new MyTextbox(pegawai.getNama() == null ? "" : pegawai.getNama()));
		nama.setWidth("90%");

		Date sekarang = WaktuUtil.getDate();
		List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkatData(sekarang);

		for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
			if (kenaikanPangkat.getKenaikanPangkatFungsional()) {
				MyFormRow jabatanfungsionalrow = new MyFormRow();
				jabatanfungsionalrow.setParent(rows);
				jabatanfungsionalrow.appendChild(new MyLabelConfig("Jabatan Fungsional"));
				Label jabatanFungsional;
				jabatanfungsionalrow.appendChild(jabatanFungsional = new Label());
				jabatanFungsional
						.setValue(kenaikanPangkat == null || kenaikanPangkat.getJabatanFungsional() == null ? ""
								: kenaikanPangkat.getJabatanFungsional().getNama());

				MyFormRow jabatanfungsionalrowtgl = new MyFormRow();
				jabatanfungsionalrowtgl.setParent(rows);
				jabatanfungsionalrowtgl.appendChild(new MyLabelConfig("Mulai Menjabat Fungsional"));
				jabatanfungsionalrowtgl
						.appendChild(new Label(kenaikanPangkat == null || kenaikanPangkat.getMulai() == null ? ""
								: Common.dateFormat6.get().format(kenaikanPangkat.getMulai())));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tunjangan Fungsional"));
				row.appendChild(new Label(kenaikanPangkat == null || kenaikanPangkat.getJabatanFungsional() == null ? ""
						: (kenaikanPangkat.getJabatanFungsional().ambilTunjangan(sekarang))));

				if (kenaikanPangkat != null && kenaikanPangkat.getKenaikanPangkatFungsional()) {
					Common.initKeterangan(rows, "Parameter tunjangan fungsional adalah TUNJ_FUNG");
				}
			}
		}

		for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
			if (kenaikanPangkat.getKenaikanPangkatGolongan()) {
				final MyFormRow jabatanstrukturalrow = new MyFormRow();
				jabatanstrukturalrow.setParent(rows);
				jabatanstrukturalrow.appendChild(new MyLabelConfig("Jabatan Struktural"));
				Label jabatanStruktural;
				jabatanstrukturalrow.appendChild(jabatanStruktural = new Label());
				jabatanStruktural
						.setValue(kenaikanPangkat == null || kenaikanPangkat.getJabatanStruktural() == null ? ""
								: kenaikanPangkat.getJabatanStruktural().getNama());
				jabatanStruktural.setWidth("90%");

				MyFormRow jabatanstrukturalrowtgl = new MyFormRow();
				jabatanstrukturalrowtgl.setParent(rows);
				jabatanstrukturalrowtgl.appendChild(new MyLabelConfig("Mulai Menjabat Struktural"));
				jabatanstrukturalrowtgl
						.appendChild(new Label(kenaikanPangkat == null || kenaikanPangkat.getMulai() == null ? ""
								: Common.dateFormat6.get().format(kenaikanPangkat.getMulai())));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tunjangan Struktural"));
				row.appendChild(new Label(kenaikanPangkat == null || kenaikanPangkat.getJabatanStruktural() == null ? ""
						: (kenaikanPangkat.getJabatanStruktural().ambilTunjangan(sekarang))));

				if (kenaikanPangkat != null && kenaikanPangkat.getKenaikanPangkatGolongan()) {
					Common.initKeterangan(rows, "Parameter tunjangan struktural adalah TUNJ_SRTK");
				}
			}
		}

		for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
			if (kenaikanPangkat.getJabatan() != null) {
				MyFormRow jabatanrow = new MyFormRow();
				jabatanrow.setParent(rows);
				jabatanrow.appendChild(new MyLabelConfig("Jabatan Lain"));
				jabatanrow.appendChild(new Label(kenaikanPangkat == null || kenaikanPangkat.getJabatan() == null ? ""
						: kenaikanPangkat.getJabatan().getNama()));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tunjangan Jabatan"));
				row.appendChild(new Label(kenaikanPangkat == null || kenaikanPangkat.getJabatan() == null ? ""
						: (kenaikanPangkat.getJabatan().ambilTunjangan(sekarang))));

				if (kenaikanPangkat != null && kenaikanPangkat.getKenaikanJabatan()) {
					Common.initKeterangan(rows, "Parameter tunjangan struktural adalah TUNJ_JAB");
				}
			}
		}

		KenaikanPangkat kenaikanPangkat = kenaikanPangkats.isEmpty() ? null : kenaikanPangkats.get(0);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Golongan"));
		row.appendChild(new Label(kenaikanPangkat == null || kenaikanPangkat.getGolongan() == null ? ""
				: kenaikanPangkat.getGolongan().getNama()));

		GajiPokok gajiPokok = pegawai.ambilGajiPokok(sekarang);
		Insentif insentif = pegawai.ambilInsentif(sekarang);
		Makan makan = pegawai.ambilMakan(sekarang);
		Transport transport = pegawai.ambilTransport(sekarang);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gaji Pokok"));
		row.appendChild(new Label(gajiPokok == null ? "" : Common.numberFormat.get().format(gajiPokok.getGaji())));

		Common.initKeterangan(rows, "Parameter gaji pokok adalah GAPOK");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Insentif"));
		row.appendChild(new Label(insentif == null ? "" : Common.numberFormat.get().format(insentif.getInsentif())));

		Common.initKeterangan(rows, "Parameter insentif adalah INSENTIF");

		jpDefault = new MyDoublebox(pegawai.getJpDefault());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jam Pelajaran (JP)"));
		row.appendChild(jpDefault);

		Common.initKeterangan(rows, "Parameter Jam Pelajaran adalah JP");

		persenKpiDefault = new MyDoublebox(pegawai.getPersenKpiDefault());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persen insentif"));

		Session session = HibernateUtil.currentSession();
		PenilaianKpi penilaianKpiData = PenilaianKpi.hitungKpi(session, pegawai, sekarang);

		row.appendChild(new MyLabelBold(
				Common.numberFormat.get().format(penilaianKpiData == null ? 0.0 : penilaianKpiData.getPersen()) + "%"
						+ (penilaianKpiData == null ? " (Belum dinilai/belum disetujui)"
								: " Berlaku " + penilaianKpiData.getTa())));

		Common.initKeterangan(rows, "Parameter insentif adalah PERSEN_INSENTIF");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Makan"));
		row.appendChild(new Label(makan == null ? "" : Common.numberFormat.get().format(makan.getMakan())));

		Common.initKeterangan(rows, "Parameter makan adalah MAKAN");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Transportasi"));
		row.appendChild(new Label(transport == null ? "" : Common.numberFormat.get().format(transport.getTransport())));

		Common.initKeterangan(rows, "Parameter transportasi adalah TRANSPORT");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lain-lain"));
		row.appendChild(new Label(gajiPokok == null ? "" : Common.numberFormat.get().format(gajiPokok.getLain())));

		Common.initKeterangan(rows, "Parameter transportasi adalah LAIN_LAIN");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tunjangan Kinerja"));
		row.appendChild(tunjanganKinerja = new MyDoublebox(pegawai.getTunjanganKinerja()));
		tunjanganKinerja.setWidth("90%");

		Common.initKeterangan(rows, "Parameter tunjangan kinerja adalah TUNJANGAN_KINERJA_KHUSUS");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Pangkat"));
		row.appendChild(pangkat = new MyTextbox(pegawai.getPangkat() == null ? "" : pegawai.getPangkat()));
		pangkat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("PTKP Pegawai"));
		ptkpPegawai = new Combobox();
		Common.insertComboDanSemua(ptkpPegawai, new String[] { "nama" }, "keterangan", PtkpPegawai.class,
				"=Belum Ditentukan=", Restrictions.eq("aktif", true));
		Common.selectComboItem(ptkpPegawai, pegawai.getPtkpPegawai());
		row.appendChild(ptkpPegawai);

		ptkpPegawai.setWidth("90%");

		Common.initKeterangan(rows, "Parameter tarif PTKP pegawai adalah PTKP_PEGAWAI");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Pendidikan Terakhir"));
		row.appendChild(pendidikan = new Combobox());
		Common.insertCombo(pendidikan, "nama", Pendidikan.class);
		Common.selectComboItem(pendidikan, pegawai.getPendidikan());
		pendidikan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Deskripsi Pendidikan Terakhir"));
		row.appendChild(deskripsiPendidikan = new MyTextbox(
				pegawai.getDeskripsiPendidikan() == null ? "" : pegawai.getDeskripsiPendidikan()));
		deskripsiPendidikan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Status Pegawai"));
		row.appendChild(status = new Combobox());
		Common.insertCombo(status, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(status, pegawai.getStatusPegawai());
		status.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Telp./Hp."));
		row.appendChild(telp = new MyTextbox(pegawai.getTelp() == null ? "" : pegawai.getTelp()));
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Alamat"));
		row.appendChild(alamat = new MyTextbox(pegawai.getAlamat() == null ? "" : pegawai.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox(pegawai.getKeterangan() == null ? "" : pegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
	}

	private void init(final Pegawai pegawai) throws Exception {
		this.pegawai = pegawai;
		addWindow.setTitle(pegawai.getId() == null ? "Tambah Pegawai" : "Ubah Pegawai");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		if (pegawai.getId() != null) {
			east = new East();
			east.setWidth("75%");
			ais.ui.util.ZkCompat.setFlex(east, true);
			east.setParent(borderlayout);
			addWindow.setHeight("97%");
			addWindow.setWidth("97%");

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(east);
			tabbox.setHeight("5000px");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tabSoal1 = new MyTabConfig("Penggajian I");
			tabSoal1.setParent(tabs);

			MyTabConfig tabSoal2 = new MyTabConfig("Penggajian II");
			tabSoal2.setParent(tabs);

			MyTabConfig tabSoal3 = new MyTabConfig("Penggajian III");
			tabSoal3.setParent(tabs);

			MyTabConfig tabSoal4 = new MyTabConfig("Penggajian IV");
			tabSoal4.setParent(tabs);

			MyTabConfig tabSoal5 = new MyTabConfig("Penggajian V");
			tabSoal5.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setParent(tabpanels);
			tabpanelUtama.setHeight("5000px");

			MyInclude include = new MyInclude(
					"/pages/master/payroll/item_gaji_pegawai.zul?pegawai=" + pegawai.getId() + "&ke=1");
			tabpanelUtama.appendChild(include);

			final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
			tabpanel2.setParent(tabpanels);
			tabpanel2.setHeight("5000px");
			tabSoal2.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel2.getChildren().isEmpty()) {
						MyInclude include = new MyInclude(
								"/pages/master/payroll/item_gaji_pegawai.zul?pegawai=" + pegawai.getId() + "&ke=2");

						Borderlayout borderlayout = new Borderlayout();

						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.appendChild(include);

						tabpanel2.appendChild(borderlayout);
					}

				}
			});

			final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
			tabpanel3.setParent(tabpanels);
			tabpanel3.setHeight("5000px");
			tabSoal3.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel3.getChildren().isEmpty()) {
						MyInclude include = new MyInclude(
								"/pages/master/payroll/item_gaji_pegawai.zul?pegawai=" + pegawai.getId() + "&ke=3");
						Borderlayout borderlayout = new Borderlayout();

						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.appendChild(include);

						tabpanel3.appendChild(borderlayout);
					}

				}
			});

			final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
			tabpanel4.setParent(tabpanels);
			tabpanel4.setHeight("5000px");
			tabSoal4.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel4.getChildren().isEmpty()) {
						MyInclude include = new MyInclude(
								"/pages/master/payroll/item_gaji_pegawai.zul?pegawai=" + pegawai.getId() + "&ke=4");
						Borderlayout borderlayout = new Borderlayout();

						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.appendChild(include);

						tabpanel4.appendChild(borderlayout);
					}

				}
			});

			final Tabpanel tabpanel5 = new ais.ui.util.MyTabpanel();
			tabpanel5.setParent(tabpanels);
			tabpanel5.setHeight("5000px");
			tabSoal5.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel5.getChildren().isEmpty()) {
						MyInclude include = new MyInclude(
								"/pages/master/payroll/item_gaji_pegawai.zul?pegawai=" + pegawai.getId() + "&ke=5");
						Borderlayout borderlayout = new Borderlayout();

						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.appendChild(include);

						tabpanel5.appendChild(borderlayout);
					}

				}
			});

		} else {
			addWindow.setHeight("97%");
			addWindow.setWidth("350px");
		}

		Tabbox tabbox = new Tabbox();
		tabbox.setStyle("border:0px;");
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setStyle("border:0px;");
		tabs.setParent(tabbox);

		Tab tabPasien = new Tab("Pegawai");
		tabPasien.setParent(tabs);

		Tab tabDokter = new Tab("Informasi Penggajian");
		tabDokter.setParent(tabs);

		Tab tabParameter = new Tab("Parameter Penggajian");
		tabParameter.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setStyle("border:0px;background: transparent;");
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		createMain(tabpanel, pegawai);

//		tabpanel = new ais.ui.util.MyTabpanel();
//		tabpanel.setParent(tabpanels);
//		biodata(tabpanel, pegawai);

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		informasiShift(tabpanel, pegawai);

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		informasiParameter(tabpanel, pegawai);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Nama Pegawai belum diisi. Kolom Nama Pegawai wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) mohon Bapak/Ibu mengisi kolom Nama Pegawai; (2) pastikan kolom tersebut tidak dikosongkan; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pegawai.getId() != null) {
			pegawai = (Pegawai) session.load(Pegawai.class, pegawai.getId());
		}

//		pegawai.setJumlahAnak(jumlahAnak.getValue());
		pegawai.setDepartemen(
				(Departemen) (departemen.getSelectedItem() == null || departemen.getSelectedItem().getValue() == null
						? null
						: departemen.getSelectedItem().getValue()));
		pegawai.setCabang(
				(Cabang) (cabang.getSelectedItem() == null || cabang.getSelectedItem().getValue() == null ? null
						: cabang.getSelectedItem().getValue()));
		pegawai.setLevelJabatan((LevelJabatan) (levelJabatan.getSelectedItem() == null
				|| levelJabatan.getSelectedItem().getValue() == null ? null
						: levelJabatan.getSelectedItem().getValue()));
		pegawai.setDeskripsiPendidikan(deskripsiPendidikan.getValue().trim());
		pegawai.setAlamat(alamat.getValue());
		pegawai.setKode(kode.getValue().trim());
		pegawai.setPangkat(pangkat.getValue().trim());
		pegawai.setPendidikan(
				(Pendidikan) (pendidikan.getSelectedItem() == null ? null : pendidikan.getSelectedItem().getValue()));
		pegawai.setStatusPegawai(
				(StatusPegawai) (status.getSelectedItem() == null || status.getSelectedItem().getValue() == null ? null
						: status.getSelectedItem().getValue()));
		pegawai.setTelp(telp.getValue().trim());
		pegawai.setNama(nama.getValue());
		pegawai.setKeterangan(keterangan.getValue());
		pegawai.setNorek(norek.getValue());
		pegawai.setNorek2(norek2.getValue());
		pegawai.setNorek3(norek3.getValue());
		pegawai.setNorek4(norek4.getValue());
		pegawai.setNorek5(norek5.getValue());
		
		
		pegawai.setBank((Bank) (bank.getSelectedItem() == null ? null : bank.getSelectedItem().getValue()));
		pegawai.setBank2(
				(Bank) (bank2 == null || bank2.getSelectedItem() == null ? null : bank2.getSelectedItem().getValue()));

		pegawai.setBank3(
				(Bank) (bank3 == null || bank3.getSelectedItem() == null ? null : bank3.getSelectedItem().getValue()));
		
		pegawai.setBank4(
				(Bank) (bank4 == null || bank4.getSelectedItem() == null ? null : bank4.getSelectedItem().getValue()));
		
		pegawai.setBank5(
				(Bank) (bank5 == null || bank5.getSelectedItem() == null ? null : bank5.getSelectedItem().getValue()));
		
		
		
		
		pegawai.setDitransferAtasNama(ditransferAtasNama.getValue());

		pegawai.setDitransferAtasNama2(ditransferAtasNama2.getValue());
		pegawai.setDitransferAtasNama3(ditransferAtasNama3.getValue());
		
		pegawai.setDitransferAtasNama4(ditransferAtasNama4.getValue());
		pegawai.setDitransferAtasNama5(ditransferAtasNama5.getValue());

		pegawai.setCaraPembayaran(caraPembayaran.getValue());
		pegawai.setCaraPembayaran2(caraPembayaran2.getValue());
		pegawai.setCaraPembayaran3(caraPembayaran3.getValue());
		
		pegawai.setCaraPembayaran4(caraPembayaran4.getValue());
		pegawai.setCaraPembayaran5(caraPembayaran5.getValue());
		

//		pegawai.setKelamin(
//				(String) (jenisKelamin.getSelectedItem() == null ? null : jenisKelamin.getSelectedItem().getValue()));
//		pegawai.setTanggallahir(tanggalLahir.getValue());
//		pegawai.setTempatlahir(tempatLahir.getValue());
//		pegawai.setAgama((Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));
		pegawai.setJamsostek(jamsostek.getValue());
//		pegawai.setStatusPerkawinan((String) (statusPerkawinan.getSelectedItem() == null ? null
//				: statusPerkawinan.getSelectedItem().getValue()));
		pegawai.setTunjanganKinerja(tunjanganKinerja.getValue());

		pegawai.setPtkpPegawai((PtkpPegawai) (ptkpPegawai.getSelectedItem() == null ? null
				: ptkpPegawai.getSelectedItem().getValue()));

		if (persenKpiDefault != null) {
			pegawai.setPersenKpiDefault(persenKpiDefault.getValue());
		}
		if (jpDefault != null) {
			pegawai.setJpDefault(jpDefault.getValue());
		}

		pegawai.setJenisGajiPegawai((JenisGajiPegawai) (jenisGajiPegawai.getSelectedItem() == null ? null
				: jenisGajiPegawai.getSelectedItem().getValue()));

		if (parameterTambahanListener != null)
			parameterTambahanListener.onSave(pegawai);

		Common.refreshSaveOrUpdate(session, pegawai);
		return true;
	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

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
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)))

				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mycode", searchkode.getValue().trim(), MatchMode.ANYWHERE)))

				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)))

				.add((searchjabatan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchjabatan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("jabatan", searchjabatan.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchcabang.getSelectedItem() == null || searchcabang.getSelectedItem().getValue() == null
						|| searchcabang.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("cabang", searchcabang.getSelectedItem().getValue()))

				.add(searchdepartemen.getSelectedItem() == null || searchdepartemen.getSelectedItem().getValue() == null
						|| searchdepartemen.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("departemen", searchdepartemen.getSelectedItem().getValue()))

				.add(searchlevelJabatan.getSelectedItem() == null
						|| searchlevelJabatan.getSelectedItem().getValue() == null
						|| searchlevelJabatan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("levelJabatan", searchlevelJabatan.getSelectedItem().getValue()))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						|| searchstatus.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()));

		if (order)
			criteria.addOrder(Order.asc("nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<Pegawai> pegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pegawai);
		grid.setRowRenderer(new PegawaiRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

}
