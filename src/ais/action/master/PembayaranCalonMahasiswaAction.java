package ais.action.master;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataCalonMahasiswaDaftarUlangBaruBanbox;
import ais.action.master.helper.generic.AmbilDataItemBiayaBanyak;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPMB;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.KegiatanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogPembayaran;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.action.master.helper.FilterLanjutHelper;

public class PembayaranCalonMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4681108885695239730L;

	private Combobox akun;
	private MyDatebox tanggalValidasi;

	private Row rowNim;
	private Row rowNama;
	private Row rowKewarganegaraan;
	private Row rowJenisKuliah;
	private Row rowProdi;
	private Row rowTahunMasuk;
	private Row rowTahunAkademik;
	private Row rowTanggalValidasi;
	private Row rowValidator;
	private Row rowPengurangan;
	private Row rowKeterangan;
	private Row rowListBiaya;
	private Row rowButtonSave;

	private AmbilDataCalonMahasiswaDaftarUlangBaruBanbox pilihCalon;
	private Label kewarganegaraan;
	private Label jenisKuliah;
	private Label prodi;
	private Label labelNimBiodataCalonMahasiswa;
	private Label labelNamaBiodataCalonMahasiswa;
	private Label labelTahunMasuk;
	private Label labelTahunAkademik;
	private Label validator;
	private MyDoublebox pengurangan;
	private Textbox keterangan;
	private MyGrid gridss;
	private MyButtonConfig save;

	Label labelFooter1;
	Label labelFooter2;

	private Combobox jenisPembayaran;

	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private Kegiatan kegiatan;
	private PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	private Double nilaiBiayaHarusDiBayars = 0.0;

	private List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
	private Center center;

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

		if (save != null) {
			String labelBayar = Common.getBahasa("label_bayar");
			save.setLabel(labelBayar);
		}

		jenisPembayaran = Common.initJenisPembayaranBiodataCalonMahasiswa(jenisPembayaran);

		// FIX NPE doAfterCompose: 'save' di-autowire dari zul & bisa null pada varian zul/domain
		// tertentu (sudah di-guard serupa "if (save != null)" saat setLabel di atas). Pasang listener
		// hanya bila tombol memang ada, agar compose (via Include.afterCompose) tak melempar NPE.
		if (save != null) save.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show(
						"Apakah yakin ingin melakukan pembayaran untuk:\nBiodataCalonMahasiswa : "
								+ biodataCalonMahasiswa.getNama() + "\nJumlah : " + labelFooter2.getValue() + " ",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									if (onSave(kegiatan, biodataCalonMahasiswa, event)) {
										Common.freeze(center, true);
									}

								}

							}
						});

			}
		});

	        FilterLanjutHelper.setup(comp);
}

	private void reset() {
		// FIX NPE reset(): Row di-autowire dari zul; pada varian zul/domain tertentu sebagian Row bisa
		// tidak ada (null). Sembunyikan secara null-safe agar reset() tidak melempar NullPointerException.
		setVisibleSafe(rowNim, false);
		setVisibleSafe(rowNama, false);
		setVisibleSafe(rowKewarganegaraan, false);
		setVisibleSafe(rowJenisKuliah, false);
		setVisibleSafe(rowProdi, false);
		setVisibleSafe(rowTahunMasuk, false);
		setVisibleSafe(rowTahunAkademik, false);
		setVisibleSafe(rowTanggalValidasi, false);
		setVisibleSafe(rowValidator, false);
		setVisibleSafe(rowPengurangan, false);
		setVisibleSafe(rowKeterangan, false);
		setVisibleSafe(rowListBiaya, false);
		setVisibleSafe(rowButtonSave, false);
	}

	private static void setVisibleSafe(Row r, boolean visible) {
		if (r != null) {
			r.setVisible(visible);
		}
	}

	private EventListener eventListener = new EventListener() {

		@Override
		public void onEvent(Event event) throws Exception {

			Common.clear(rowListBiaya);

			String tahunAkademik = Common.getCurrentTahunAkademik();

			JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
					: jenisPembayaran.getSelectedItem().getValue());

			labelTahunAkademik.setValue(tahunAkademik);
			rowListBiaya.setVisible(true);

			kegiatan = biodataCalonMahasiswa.ambilKegiatans(jenisPembayaran.getSelectedItem() == null ? 0
					: jenisPembayaran.getSelectedItem().getValue().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA)
							? 0
							: 1,
					jenisKegiatan);

			listBiaya(rowListBiaya, biodataCalonMahasiswa, kegiatan);
		}
	};

	private Hbox hboxJenisPembayaran;

	public void onCariBiodataCalonMahasiswa(Event event) throws Exception {

		if (jenisPembayaran.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Pembayaran",
					"Kolom Jenis Pembayaran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Pembayaran.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			reset();
			return;
		}
		JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
				: jenisPembayaran.getSelectedItem().getValue());

		try {
			biodataCalonMahasiswa = (BiodataCalonMahasiswa) pilihCalon.getAttribute("calonMahasiswa");

			if (biodataCalonMahasiswa == null) {
				MyMessageboxConfig.show("Calon Mahasiswa tidak terdaftar", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				reset();
				return;
			}

			else {

				kegiatan = biodataCalonMahasiswa
						.ambilKegiatans(
								jenisPembayaran.getSelectedItem() == null ? 0
										: jenisPembayaran.getSelectedItem().getValue()
												.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA) ? 0 : 1,
								jenisKegiatan);

				rowNim.setVisible(true);
				labelNimBiodataCalonMahasiswa.setValue(biodataCalonMahasiswa.getNoRegistrasi());
				rowNama.setVisible(true);
				labelNamaBiodataCalonMahasiswa.setValue(biodataCalonMahasiswa.getNama());
				rowKewarganegaraan.setVisible(true);
				kewarganegaraan.setValue(biodataCalonMahasiswa.getKewarganegaraan());

				rowJenisKuliah.setVisible(true);
				jenisKuliah.setValue("Reguler");
				rowProdi.setVisible(true);
				prodi.setValue(biodataCalonMahasiswa.getProdiLulus() == null
						? biodataCalonMahasiswa.getProdi1() == null
								? biodataCalonMahasiswa.getProdi2() == null ? ""
										: biodataCalonMahasiswa.getProdi2().getNama()
								: biodataCalonMahasiswa.getProdi1().getNama()
						: biodataCalonMahasiswa.getProdiLulus().getNama());

				rowTahunMasuk.setVisible(true);
				labelTahunMasuk.setValue(biodataCalonMahasiswa.getTahun().toString());
				rowTahunAkademik.setVisible(true);

				tanggalValidasi.setDisabled(false);
				rowTanggalValidasi.setVisible(true);
				rowValidator.setVisible(true);
				rowPengurangan.setVisible(true);
				rowKeterangan.setVisible(true);

				/*
				 * if (jenisKegiatanDetail != null)
				 * tanggalValidasi.setValue(kegiatan.getTanggal()); else
				 */
				if (kegiatan != null) {
					// Common.freeze(center, true);
					tanggalValidasi.setValue(kegiatan.getTanggal());
					validator.setValue(kegiatan.getValidator() == null ? "" : kegiatan.getValidator());

					keterangan.setValue(kegiatan.getKeterangan() == null ? "" : kegiatan.getKeterangan());
				} else
					tanggalValidasi.setValue(ais.ui.util.WaktuUtil.getDate());
				// rowButton.setVisible(true);

				Common.clear(rowListBiaya);
				rowListBiaya.setVisible(true);
				eventListener.onEvent(null);

			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	@SuppressWarnings({ "unchecked" })
	public void listBiaya(final Component comp, final BiodataCalonMahasiswa biodataCalonMahasiswa, final Kegiatan keg)
			throws Exception {
		kegiatan = keg;
		detailBiayas = new ArrayList<DetailBiaya>();
		final JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
				: jenisPembayaran.getSelectedItem().getValue());

		this.biodataCalonMahasiswa = biodataCalonMahasiswa;

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.appendChild(new MyCaptionStyled("Daftar Biaya"));

		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("95%");
		groupbox.setParent(comp);
		SatuanKerja satuanKerja = Common.getSatuanKerja();
		Common.insertCombo(akun = new Combobox(), "nama", "akun", JenisPembayaran.class,
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		hboxJenisPembayaran = new Hbox(new Component[] { new Label(ais.common.Common.getBahasaConfig("Cara Pembayaran : ")), akun });
		akun.setCols(50);
		hboxJenisPembayaran.setParent(groupbox);

		if (kegiatan != null && kegiatan.getId() != null) {
			CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) HibernateUtil.currentSession()
					.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("kegiatan", kegiatan))
					.addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
			if (cicilanPembayaran != null) {
				Common.selectComboItem(akun, cicilanPembayaran.getJenisPembayaran());
			} else {
				Common.selectComboItem(akun, ConstantValues.TUNAI);
			}
		} else {
			Common.selectComboItem(akun, ConstantValues.TUNAI);
		}

		if (akun != null && akun.getSelectedItem() == null) {
			JenisPembayaran jenisPembayaranDefault = (JenisPembayaran) HibernateUtil.currentSession()
					.createCriteria(JenisPembayaran.class).add(Restrictions.eq("defaultPembayaran", true))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
					.uniqueResult();
			Common.selectComboItem(akun, jenisPembayaranDefault);
		}

		hboxJenisPembayaran.setVisible(Common.bolehKonfigurasi("integrasi_modul_akuntansi", Konfigurasi.TIDAK_AKTIF));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Item Biaya", "/img/new.gif");

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				List<ItemBiaya> itemBiayas = new ArrayList<ItemBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					itemBiayas.add(detailBiaya.getItemBiaya());
				}

				AmbilDataItemBiayaBanyak window = new AmbilDataItemBiayaBanyak(itemBiayas);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("700px");
				window.setHeight("90%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemBiaya> itemBiayas = (List<ItemBiaya>) arg0.getData();
						if (itemBiayas != null) {
							rowButtonSave.setVisible(itemBiayas.size() != 0);

							Session session = HibernateUtil.currentSession();

							for (ItemBiaya itemBiaya : itemBiayas) {
								DetailBiaya detailBiaya = (DetailBiaya) session.createCriteria(DetailKegiatan.class)
										.createAlias("detailBiaya", "detailBiaya")
										.add(Restrictions.eq("detailBiaya.itemBiaya", itemBiaya))
										.createAlias("kegiatan", "kegiatan")
										.setProjection(Projections.property("detailBiaya"))
										.add(Restrictions.eq("kegiatan.calonMahasiswa", biodataCalonMahasiswa))
										.add(Restrictions.eq("kegiatan.jenisKegiatan", jenisKegiatan))

										.add(Restrictions.eq("kegiatan.tahunAkademik", labelTahunAkademik.getValue()))
										.setMaxResults(1).uniqueResult();

								if (detailBiaya == null) {
									detailBiaya = new DetailBiaya();
								}

								detailBiaya.setAngkatan(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
								detailBiaya
										.setFakultas(
												biodataCalonMahasiswa.getProdiLulus() == null
														? biodataCalonMahasiswa.getProdi1() == null
																? biodataCalonMahasiswa.getProdi2() == null ? null
																		: biodataCalonMahasiswa.getProdi2()
																				.getFakultas()
																: biodataCalonMahasiswa.getProdi1().getFakultas()
														: biodataCalonMahasiswa.getProdiLulus().getFakultas());
								detailBiaya.setItemBiaya(itemBiaya);
								detailBiaya.setJenisKegiatan(jenisKegiatan);
								detailBiaya.setJenisSeleksi(null);
								detailBiaya.setJenjang(biodataCalonMahasiswa.getJenjang());
								detailBiaya.setJurusan(biodataCalonMahasiswa.getProdiLulus() == null
										? biodataCalonMahasiswa.getProdi1() == null
												? biodataCalonMahasiswa.getProdi2() == null ? null
														: biodataCalonMahasiswa.getProdi2()
												: biodataCalonMahasiswa.getProdi1()
										: biodataCalonMahasiswa.getProdiLulus());
								detailBiaya.setMerupakanPembayaran(true);
								detailBiaya.setNama("Pembayaran BiodataCalonMahasiswa");
								detailBiaya.setNilaiBiaya(0.0);
								detailBiaya.setProgram(biodataCalonMahasiswa.getProgram());
								detailBiaya.setSemester(jenisPembayaran.getSelectedItem() == null ? 0
										: jenisPembayaran.getSelectedItem().getValue()
												.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA) ? 0 : 1);
								detailBiaya.setStatusMahasiswa(ConstantValues.AKTIF);
								detailBiaya.setTahunAkademik(labelTahunAkademik.getValue());
								detailBiaya.setWnaAtauWni(biodataCalonMahasiswa.getKewarganegaraan());
								detailBiayas.add(detailBiaya);

							}
							loadData(detailBiayas);

						}
					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		gridss = new MyGrid();
		gridss.setMold("paging");
		gridss.setPageSize(1000);
		gridss.setParent(groupbox);
		gridss.setWidth("100%");

		Columns columns = new Columns();
		columns.setParent(gridss);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Item Biaya");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Biaya");
		column.setWidth("25%");
		column.setAlign("right");

		Foot foot = new Foot();
		foot.setParent(gridss);

		Footer footer = new Footer();
		footer.setParent(foot);
		labelFooter1 = new Label();
		labelFooter1.setParent(footer);
		labelFooter1.setValue("Jumlah Biaya");

		footer = new Footer();
		footer.setAlign("right");
		footer.setParent(foot);
		labelFooter2 = new Label();
		labelFooter2.setParent(footer);

		if (kegiatan != null && kegiatan.getId() != null && kegiatan.getAmount() > 0.1) {
			save.setDisabled(true);
			save.setLabel("Calon Mahasiswa ini sudah melakukan pembayaran");
			MyMessageboxConfig.show("Calon Mahasiswa ini sudah melakukan pembayaran", "Pemberitahuan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);

							pembayaranUtil.updateTunggakan(kegiatan, HibernateUtil.currentSession());

						}
					});

			Session session = HibernateUtil.currentSession();
			detailBiayas = session.createCriteria(DetailKegiatan.class).createAlias("kegiatan", "kegiatan")
					.setProjection(Projections.property("detailBiaya"))
					.add(Restrictions.eq("kegiatan.calonMahasiswa", biodataCalonMahasiswa))
					.add(Restrictions.eq("kegiatan.jenisKegiatan", jenisKegiatan))
					.add(Restrictions.eq("kegiatan.tahunAkademik", labelTahunAkademik.getValue())).list();

			loadData(detailBiayas);

		} else {
			save.setDisabled(false);
			if (save != null) {
				String labelBayar = Common.getBahasa("label_bayar");
				save.setLabel(labelBayar);
			}
		}

	}

	public void loadData(List<DetailBiaya> detailBiayas) throws Exception {
		this.detailBiayas = detailBiayas;
		ListModel strset = null;
		strset = new SimpleListModel(detailBiayas);
		gridss.setRowRenderer(new DetailBiayaBiodataCalonMahasiswaRenderer());
		gridss.setModelCheckMobile(strset);
		gridss.renderAll();

		hitungJumlahBiayaSeharusnya();
	}

	class DetailBiayaBiodataCalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DetailBiaya detailBiaya = (DetailBiaya) arg1;
			arg0.setAttribute("myValue", detailBiaya);

			Session session = HibernateUtil.currentSession();

			DetailKegiatan detailKegiatan = detailBiaya == null || kegiatan == null || detailBiaya.getId() == null
					|| kegiatan.getId() == null
							? null
							: (DetailKegiatan) session.createCriteria(DetailKegiatan.class)
									.add(Restrictions.eq("detailBiaya", detailBiaya))
									.add(Restrictions.eq("kegiatan", kegiatan)).setMaxResults(1).uniqueResult();

			new Label(detailBiaya.getItemBiaya().getNama()).setParent(arg0);

			final MyDoublebox harusDiBayar = new MyDoublebox(detailKegiatan == null ? 0.0 : detailKegiatan.getBiaya());
			harusDiBayar.setWidth("90%");
			harusDiBayar.setParent(arg0);

			harusDiBayar.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					detailBiaya.setNilaiBiaya(harusDiBayar.getValue());
					hitungJumlahBiayaSeharusnya();
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Kegiatan keg, BiodataCalonMahasiswa biodataCalonMahasiswa, Event event) throws Exception {
		kegiatan = keg;
		JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
				: jenisPembayaran.getSelectedItem().getValue());

		if (jenisKegiatan == null) {
			MyMessageboxConfig.show("Jenis pembayaran harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (hboxJenisPembayaran.isVisible() && akun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Cara pembayaran harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		try {
			KegiatanDao kegiatanDao = DaoFactory.getInstance().getKegiatanDao();

			Rows rows = (Rows) gridss.getRows();

			if (kegiatan != null && kegiatan.getId() != null) {
				kegiatan = kegiatanDao.load(kegiatan.getId());
			} else {
				kegiatan = new Kegiatan();
			}

			this.biodataCalonMahasiswa = biodataCalonMahasiswa;
			kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
			// kegiatan.setProgram(mahasiswa.getProgram());
			kegiatan.setSemster(jenisPembayaran.getSelectedItem() == null ? 0
					: jenisPembayaran.getSelectedItem().getValue().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA)
							? 0
							: 1);
			kegiatan.setTahunAkademik(labelTahunAkademik.getValue() == null ? "" : labelTahunAkademik.getValue());
			kegiatan.setTanggal(
					tanggalValidasi.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : tanggalValidasi.getValue());
			kegiatan.setValidated(1);
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setValidator(Common.getCurrentUser().getUserNama());
			kegiatan.setPengurangan(pengurangan.getValue() == null ? 0.0 : pengurangan.getValue());
			kegiatan.setKeterangan(keterangan.getValue().trim());
			kegiatan.setAmount(nilaiBiayaHarusDiBayars);

			validator.setValue(kegiatan.getValidator());

			keterangan.setValue(kegiatan.getKeterangan() == null ? "" : kegiatan.getKeterangan());

			Session session = kegiatanDao.getCurrentSession();

			if (kegiatan.getId() != null) {
				kegiatanDao.update(kegiatan);
			} else {
				kegiatanDao.save(kegiatan);
			}

			if (nilaiBiayaHarusDiBayars != null && nilaiBiayaHarusDiBayars > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(nilaiBiayaHarusDiBayars);
				logPembayaran.setKeterangan("Pembayaran manual");
				Common.refreshSaveOrUpdate(logPembayaran);
			}

			if (rows != null && rows.getChildren() != null) {
				List<Row> myRows = rows.getChildren();
				for (Row row : myRows) {
					DetailBiaya detailBiaya = (DetailBiaya) row.getAttribute("myValue");

					session.saveOrUpdate(detailBiaya);

					DetailKegiatan detailKegiatan = kegiatan.ambilSatuDetailKegiatan(detailBiaya, true);
					if (detailKegiatan == null) {
						detailKegiatan = new DetailKegiatan();
					}
					Double biaya = detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
							: detailBiaya.getNilaiBiayaBaru();
					try {
						if (row.getChildren().get(1) instanceof MyDoublebox
								&& detailBiaya.getItemBiaya().getNilaiBisaDiubah()) {
							MyDoublebox jumlah = (MyDoublebox) row.getChildren().get(1);
							biaya = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
						} else if (row.getChildren().get(1) instanceof Label) {
							Label myLabel = (Label) row.getChildren().get(1);
							// System.out.println("myLabel = " +
							// myLabel.getValue());
							biaya = Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
					detailKegiatan.setBiaya(biaya);
					detailKegiatan.setDetailBiaya(detailBiaya);
					detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
					detailKegiatan.setKegiatan(kegiatan);

					Common.refreshSaveOrUpdate(session, detailKegiatan);

				}
			}

			Common.simpanCicilanTanpaMencicil(kegiatan, nilaiBiayaHarusDiBayars, tanggalValidasi.getValue(),
					keterangan.getValue(),
					(JenisPembayaran) (akun.getSelectedItem() == null ? null : akun.getSelectedItem().getValue()),
					detailBiayas, session);

			biodataCalonMahasiswa.setPembayaranRegistrasi(kegiatan);
			Common.refreshUpdate(biodataCalonMahasiswa);

			// Auto-generate No. Ujian saat pembayaran registrasi lunas.
			// Hanya berlaku untuk pembayaran pendaftaran (semster=0), bukan daftar ulang.
			// refreshUpdate di atas sudah flush ke DB, sehingga generator bisa baca state terkini.
			final String noUjianBaru = autoGenerateNoUjianJikaBelumAda(kegiatan, biodataCalonMahasiswa);

			String pesanSukses = "Pembayaran Berhasil Dilakukan";
			if (noUjianBaru != null && !noUjianBaru.trim().isEmpty()) {
				pesanSukses += "\n\nNo. Ujian " + noUjianBaru + " telah ditetapkan otomatis.";
			}

			final String pesanSuksesFinal = pesanSukses;
			MyMessageboxConfig.show(pesanSuksesFinal, "Pemberitahuan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (Common.bolehKonfigurasi("cetak_bukti_pembayaran_setelah_proses_pembayaran")) {
								CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);

							}
						}
					});

			session.flush();
			pembayaranUtil.updateTunggakan(kegiatan, HibernateUtil.currentSession());

			return true;
		} catch (Exception e) {
			MyMessageboxConfig.show("Pembayaran Gagal Dilakukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			Common.tampilErrorJikaAdmin(e);
			return false;
		}

	}

	/**
	 * Generate No. Ujian otomatis setelah pembayaran registrasi calon mahasiswa.
	 * Dipanggil hanya saat semster=0 (PENDAFTARAN_CALON_MAHASISWA) dan noUjian belum ada.
	 * Error dari generator ditangkap agar tidak mengganggu alur sukses pembayaran.
	 *
	 * @return noUjian yang baru digenerate, atau string kosong jika gagal/tidak berlaku
	 */
	private String autoGenerateNoUjianJikaBelumAda(Kegiatan kegiatan,
			BiodataCalonMahasiswa biodataCalonMahasiswa) {
		// Hanya untuk pembayaran registrasi pendaftaran (semster=0), bukan daftar ulang
		if (kegiatan == null || kegiatan.getSemster() != 0) {
			return "";
		}
		// Idempoten: skip jika noUjian sudah ada
		String existing = biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getNoUjian();
		if (existing != null && !existing.trim().isEmpty()) {
			return "";
		}
		try {
			String noUjian = CommonPMB.generateNoUjian(Common.getCurrentUser(), biodataCalonMahasiswa);
			return noUjian == null ? "" : noUjian.trim();
		} catch (Exception e) {
			// Jangan gagalkan proses pembayaran; log saja
			System.err.println("[NoUjianAutoGen] " + e.getMessage());
			return "";
		}
	}

	public void hitungJumlahBiayaSeharusnya() throws ParseException {

		Rows rows = (Rows) gridss.getRows();
		nilaiBiayaHarusDiBayars = 0.0;
		if (rows != null && rows.getChildren() != null) {
			for (int i = 0; i < rows.getChildren().size(); i++) {
				Row myRow = (Row) rows.getChildren().get(i);
				if (myRow.getChildren().get(2) instanceof Label) {
					Label myLabel = (Label) myRow.getChildren().get(2);
					// System.out.println("myLabel = " + myLabel.getValue());
					Double nilaiBiayas = Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
					nilaiBiayaHarusDiBayars += (myLabel.getValue() == null ? 0.0 : nilaiBiayas);

				}
			}
			labelFooter2.setStyle("text-align: right;");
			labelFooter2.setValue(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars));
		}
	}

}
