package ais.action.master;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ais.action.master.prestasi.DasbordPrestasi;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.dashboard.admin.DashboardRekapPrestasiPegawaiBerdasarCabang;
import ais.action.master.dashboard.admin.DashboardRekapPrestasiPegawaiBerdasarKategori;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.format1.akademik.LaporanPrestasiPegawai;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CabangPrestasiPegawai;
import ais.database.model.GeneralValueObject;
import ais.database.model.KategoriPrestasiPegawai;
import ais.database.model.Pegawai;
import ais.database.model.PrestasiPegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PrestasiPegawaiAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchpenyelenggara;
	private AmbilDataPegawaiBanbox searchpegawai;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;
	private Combobox searchstatus;
	private Combobox searchcabangPrestasiPegawai;
	private Combobox searchkategoriPrestasiPegawai;

	private Textbox nama;
	private MyDatebox tanggal;
	private MyDatebox tanggalSelesai;
	private AmbilDataPegawaiBanbox pegawai;
	private Textbox keterangan;

	private PrestasiPegawai prestasiPegawai;
	private MyToolbarbuttonConfig add;

	protected LampiranLain lainPegawai;
	private Tbmuser tbmuser;
	private Textbox tempat;
	private Textbox juara;
	private Intbox peringkat;
	private Textbox penyelenggara;
	private Textbox nomorSertifikat;

	private MyToolbarbuttonConfig uploadData;

	private Combobox cabangPrestasiPegawai;
	private Combobox kategoriPrestasiPegawai;
	private Textbox jumlahPeserta;
	private Textbox capaian;
	private Textbox url;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private List<Long> punyaBawahan = null;
	private List<Long> punyaBawahanDosen = null;

	private Pegawai mhs;
	private Tabpanel tabDasbor;

	public void onDasbor(Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordPrestasi dasbord = new DasbordPrestasi(DasbordPrestasi.Lingkup.PEGAWAI);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Prestasi Pegawai",
				"Catatan pencapaian dan penghargaan pegawai.");
		}
	}

	private Tabpanel kategoriPrestasiPegawaiTab;

	private MyColumnConfig colNama;

	public void onKategoriPrestasiPegawai(Event event) {
		if (kategoriPrestasiPegawaiTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kategoriPrestasiPegawaiTab);
			MyInclude iframe = new MyInclude("/pages/master/kategori_prestasi_pegawai.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel cabangPrestasiPegawaiTab;

	public void onCabangPrestasiPegawai(Event event) {
		if (cabangPrestasiPegawaiTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(cabangPrestasiPegawaiTab);
			MyInclude iframe = new MyInclude("/pages/master/cabang_prestasi_pegawai.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel cabangRekapTab;

	public void onRekapCabang(Event event) {
		if (cabangRekapTab.getChildren().size() == 0) {
			DashboardRekapPrestasiPegawaiBerdasarCabang window = new DashboardRekapPrestasiPegawaiBerdasarCabang();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, cabangRekapTab,
				"Rekap per Cabang", "Sebaran prestasi pegawai berdasarkan cabang ilmu atau bidang keahlian.");
		}
	}

	private Tabpanel kategoriRekapTab;
	private Intbox tahun;
	private PrestasiPegawai prestasiPegawaiSelected = null;
	private Textbox namaEn;

	public void onRekapKategori(Event event) {
		if (kategoriRekapTab.getChildren().size() == 0) {
			DashboardRekapPrestasiPegawaiBerdasarKategori window = new DashboardRekapPrestasiPegawaiBerdasarKategori();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, kategoriRekapTab,
				"Rekap per Kategori", "Sebaran prestasi pegawai berdasarkan kategori kompetisi atau penghargaan.");
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.ambilPegawai() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("pegawai") && colNama != null) {
			colNama.setWidth("0px");
		}

		kategoriPrestasiPegawaiTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.ambilPegawai() == null && tbmuser.ambilPegawai() == null);
		cabangPrestasiPegawaiTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.ambilPegawai() == null && tbmuser.ambilPegawai() == null);
		cabangRekapTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.ambilPegawai() == null && tbmuser.ambilPegawai() == null);

		kategoriRekapTab.getLinkedTab()
				.setVisible(tbmuser != null && tbmuser.ambilPegawai() == null && tbmuser.ambilPegawai() == null);

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(KategoriPrestasiPegawai.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {
			KategoriPrestasiPegawai kategoriPrestasiPegawai = new KategoriPrestasiPegawai();
			kategoriPrestasiPegawai.setNama("Internasional");
			session.save(kategoriPrestasiPegawai);

			kategoriPrestasiPegawai = new KategoriPrestasiPegawai();
			kategoriPrestasiPegawai.setNama("Nasional");
			session.save(kategoriPrestasiPegawai);

			kategoriPrestasiPegawai = new KategoriPrestasiPegawai();
			kategoriPrestasiPegawai.setNama("Regional");
			session.save(kategoriPrestasiPegawai);

			kategoriPrestasiPegawai = new KategoriPrestasiPegawai();
			kategoriPrestasiPegawai.setNama("Lain-Lain");
			session.save(kategoriPrestasiPegawai);
		}

		count = ((Number) session.createCriteria(CabangPrestasiPegawai.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			CabangPrestasiPegawai cabangPrestasiPegawai = new CabangPrestasiPegawai();
			cabangPrestasiPegawai.setNama("Seni");
			session.save(cabangPrestasiPegawai);

			cabangPrestasiPegawai = new CabangPrestasiPegawai();
			cabangPrestasiPegawai.setNama("Olah Raga");
			session.save(cabangPrestasiPegawai);

			cabangPrestasiPegawai = new CabangPrestasiPegawai();
			cabangPrestasiPegawai.setNama("Kejuaraan Ilmiah");
			session.save(cabangPrestasiPegawai);

			cabangPrestasiPegawai = new CabangPrestasiPegawai();
			cabangPrestasiPegawai.setKode("9");
			cabangPrestasiPegawai.setNama("Lain-Lain");
			session.save(cabangPrestasiPegawai);
		}

		Common.insertComboDanSemua(searchkategoriPrestasiPegawai, "nama", KategoriPrestasiPegawai.class);
		Common.insertComboDanSemua(searchcabangPrestasiPegawai, "nama", CabangPrestasiPegawai.class);

		Comboitem comboitem = new Comboitem(PrestasiPegawai.BELUM_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PrestasiPegawai.BELUM_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PrestasiPegawai.SEDANG_DIPROSES);
		if (comboitem != null) { comboitem.setValue(PrestasiPegawai.SEDANG_DIPROSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PrestasiPegawai.DISETUJUI);
		if (comboitem != null) { comboitem.setValue(PrestasiPegawai.DISETUJUI); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem(PrestasiPegawai.DITOLAK);
		if (comboitem != null) { comboitem.setValue(PrestasiPegawai.DITOLAK); }
		searchstatus.appendChild(comboitem);

		comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchstatus.appendChild(comboitem);
		if (searchstatus != null) { searchstatus.setReadonly(true); }
		if (searchstatus != null) { searchstatus.setSelectedItem(comboitem); }

		searchpegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (execution.getParameter("pegawai") != null) {
			mhs = (Pegawai) HibernateUtil.currentSession().createCriteria(Pegawai.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("pegawai")))).uniqueResult();
		} else {
			mhs = tbmuser == null ? null : tbmuser.ambilPegawai();
		}

		if (execution.getParameter("cabangPrestasiPegawai") != null) {
			CabangPrestasiPegawai cabangPrestasiPegawaiSelected = (CabangPrestasiPegawai) HibernateUtil.currentSession()
					.createCriteria(CabangPrestasiPegawai.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("cabangPrestasiPegawai"))))
					.uniqueResult();
			Common.selectComboItem(true, searchcabangPrestasiPegawai, cabangPrestasiPegawaiSelected);
		}

		if (execution.getParameter("kategoriPrestasiPegawai") != null) {
			KategoriPrestasiPegawai kategoriPrestasiPegawaiSelected = (KategoriPrestasiPegawai) HibernateUtil
					.currentSession().createCriteria(KategoriPrestasiPegawai.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("kategoriPrestasiPegawai"))))
					.uniqueResult();
			Common.selectComboItem(true, searchkategoriPrestasiPegawai, kategoriPrestasiPegawaiSelected);
		}

		if (mhs != null) {
			searchpegawai.setAttribute("pegawai", mhs);
			searchpegawai.setDisabled(true);
			searchpegawai.setValue(mhs.getNama());
		}

		if (execution.getParameter("prestasi") != null) {
			prestasiPegawaiSelected = (PrestasiPegawai) GeneralValueObject.ambilData(PrestasiPegawai.class,
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

		String[] contents = new String[] { "id", "pegawai", "nama", "namaEn", "tempat", "penyelenggara", "juara",
				"peringkat", "tanggal", "tanggalSelesai", "nomorSertifikat", "cabangPrestasiPegawai",
				"kategoriPrestasiPegawai", "jumlahPeserta", "capaian", "url", "tahunAkademik", "jenisSemester", "tahun",
				"status", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PrestasiPegawai.class, contents);
		upload.setVisible(
				(add != null && add.isVisible()) && tbmuser != null && tbmuser.ambilPegawai() == null && tbmuser.ambilPegawai() == null);
		Common.appendKeToolbar(upload, add, comp);

		if (add != null) { add.setVisible(tbmuser != null); }
		if (uploadData != null) { uploadData.setVisible(upload.isVisible()); }

		if (mhs != null) {

		}

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Prestasi Pegawai", "/img/print.png");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPrestasiPegawai laporan = new LaporanPrestasiPegawai();
				laporan.setTitle("Prestasi Pegawai");
				laporan.setClosable(true);
				laporan.setHeight("95%");
				laporan.setWidth("90%");
				laporan.setParent(page.getFirstRoot());
				laporan.onModal();
			}
		});
		if (cetak != null) { cetak.setParent(add.getParent()); }

		if (!Common.getApakahAdmin()) {
			if (tbmuser != null && tbmuser.getPegawai() != null) {

				Criterion criterion = Restrictions.or(Restrictions.eq("atasanlangsung", tbmuser.getPegawai()),
						Restrictions.or(Restrictions.eq("atasanlangsung3", tbmuser.getPegawai()),
								Restrictions.eq("atasanlangsung2", tbmuser.getPegawai())));

				Criteria criteria = session.createCriteria(Pegawai.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

				if (tbmuser.getDosen() != null) {
					criteria.createAlias("atasanlangsung", "atasanlangsung", Criteria.LEFT_JOIN)
							.createAlias("atasanlangsung2", "atasanlangsung2", Criteria.LEFT_JOIN)
							.createAlias("atasanlangsung3", "atasanlangsung3", Criteria.LEFT_JOIN);

					criterion = Restrictions.or(criterion,
							Restrictions.or(Restrictions.eq("atasanlangsung.dosen", tbmuser.getDosen()),
									Restrictions.or(Restrictions.eq("atasanlangsung3.dosen", tbmuser.getDosen()),
											Restrictions.eq("atasanlangsung2.dosen", tbmuser.getDosen()))));
				} else if (tbmuser.getGuru() != null) {
					criteria.createAlias("atasanlangsung", "atasanlangsung", Criteria.LEFT_JOIN)
							.createAlias("atasanlangsung2", "atasanlangsung2", Criteria.LEFT_JOIN)
							.createAlias("atasanlangsung3", "atasanlangsung3", Criteria.LEFT_JOIN);

					criterion = Restrictions.or(criterion,
							Restrictions.or(Restrictions.eq("atasanlangsung.guru", tbmuser.getGuru()),
									Restrictions.or(Restrictions.eq("atasanlangsung3.guru", tbmuser.getGuru()),
											Restrictions.eq("atasanlangsung2.guru", tbmuser.getGuru()))));
				}

				punyaBawahan = criteria.add(criterion).setProjection(Projections.property("id")).list();

				System.out.println("punyaBawahan -> " + punyaBawahan);

				if (tbmuser.hakAkses() != null && tbmuser.hakAkses().getJenisJabatan() != null) {
					List<Long> bawahanJabatan = session.createCriteria(Pegawai.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.or(Restrictions.eq("atasan.id", tbmuser.hakAkses().getJenisJabatan().getId()), Restrictions.or(Restrictions.eq("atasanPendukung.id", tbmuser.hakAkses().getJenisJabatan().getId()), Restrictions.eq("atasanPendukungCadangan.id", tbmuser.hakAkses().getJenisJabatan().getId()))) )
							.setProjection(Projections.property("id")).list();
					System.out.println("bawahanJabatan -> " + bawahanJabatan);
					if (!bawahanJabatan.isEmpty()) {
						punyaBawahan.addAll(bawahanJabatan);
					}
				}

				else {

					List<Tbmrole> roles = tbmuser.ambilRoles();
					boolean ada = false;
					for (Tbmrole tbmrole : roles) {
						if (tbmrole != null && tbmrole.getJenisJabatan() != null) {
							ada = true;
							break;
						}
					}

					if (!ada) {
						List<Long> pejabats = session.createCriteria(Pejabat.class)

								.setProjection(Projections.groupProperty("jenisJabatan.id"))

								.add(Restrictions.or(
										Restrictions.or(
												Restrictions.ilike("jenisPengguna",
														"," + tbmuser.hakAkses().getRoleId() + ",", MatchMode.ANYWHERE),
												Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
														MatchMode.ANYWHERE)),
										Restrictions.and(
												Restrictions.or(Restrictions.isNotNull("pegawai"),
														Restrictions.or(Restrictions.isNotNull("guru"),
																Restrictions.isNotNull("dosen"))),
												Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
														Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
																Restrictions.eq("guru", tbmuser.getGuru()))))))

								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

						System.out.println("pejabats -> " + pejabats);
						if (!pejabats.isEmpty()) {
							List<Long> bawahanJabatan = session.createCriteria(Pegawai.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.or(Restrictions.in("atasan.id", pejabats), Restrictions.or( Restrictions.in("atasanPendukung.id", pejabats),  Restrictions.in("atasanPendukungCadangan.id", pejabats))))
									.setProjection(Projections.property("id")).list();
							System.out.println("bawahanJabatan -> " + bawahanJabatan);
							if (!bawahanJabatan.isEmpty()) {
								punyaBawahan.addAll(bawahanJabatan);
							}
						}
					}
				}

				if (!tbmuser.hakAkses().getMelihatDataPegawaiLain()) {
					punyaBawahan.add(tbmuser.getPegawai().getId());
				}
				System.out.println("punyaBawahan -> " + punyaBawahan);

			}

			if (tbmuser != null && tbmuser.getDosen() != null) {
				punyaBawahanDosen = HibernateUtil.currentSession().createCriteria(Pegawai.class)
						.createAlias("dosen", "dosen").add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.eq("atasanlangsung", tbmuser.getPegawai()),
								Restrictions.or(Restrictions.eq("atasanlangsung3", tbmuser.getPegawai()),
										Restrictions.eq("atasanlangsung2", tbmuser.getPegawai()))))
						.setProjection(Projections.groupProperty("dosen.id")).list();

				punyaBawahanDosen.add(tbmuser.getDosen().getId());
				System.out.println("punyaBawahanDosen -> " + punyaBawahanDosen);
			}

		}
	}

	class PrestasiPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PrestasiPegawai prestasiPegawai = (PrestasiPegawai) arg1;

			try {
				if (prestasiPegawaiSelected != null
						&& prestasiPegawaiSelected.getId().equals(prestasiPegawai.getId())) {
					arg0.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PrestasiPegawaiAction.java:502");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);

			Vbox myvbox = new Vbox();
			myvbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(prestasiPegawai.getPegawai()).setParent(myvbox);

			new Label(prestasiPegawai.getPegawai().getNama()).setParent(myvbox);

			Vbox a = RevisiHelper.createNewRevisi(PrestasiPegawai.class, prestasiPegawai, prestasiPegawai.getNama());
			new Label(prestasiPegawai.getNamaEn()).setParent(a);

			a.setParent(arg0);

			myvbox = new Vbox();
			myvbox.setParent(detail);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, prestasiPegawai.getId(), PrestasiPegawai.class.getName(),
					"Lampiran", false, null, null, false, false, false, false);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new MyLabelAgakKecil("Tempat: " + prestasiPegawai.getTempat()).setParent(myvbox);
			new MyLabelAgakKecil("Penyelenggara: " + prestasiPegawai.getPenyelenggara()).setParent(myvbox);
			new MyLabelAgakKecil("Juara: " + prestasiPegawai.getJuara()).setParent(myvbox);
			new MyLabelAgakKecil(
					"Peringkat: " + (prestasiPegawai.getPeringkat() == null ? "" : prestasiPegawai.getPeringkat()))
					.setParent(myvbox);
			new MyLabelAgakKecil("Tanggal: "
					+ (prestasiPegawai.getTanggal() == null ? ""
							: Common.dateFormat1.get().format(prestasiPegawai.getTanggal()))
					+ (prestasiPegawai.getTanggalSelesai() == null ? ""
							: " s.d " + Common.dateFormat1.get().format(prestasiPegawai.getTanggalSelesai())))
					.setParent(myvbox);
			new MyLabelAgakKecil(
					"TA/Smt: " + prestasiPegawai.getTahunAkademik() + "/" + prestasiPegawai.getJenisSemester())
					.setParent(myvbox);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new MyLabelAgakKecil("Cabang: " + (prestasiPegawai.getCabangPrestasiPegawai() == null ? ""
					: prestasiPegawai.getCabangPrestasiPegawai().getNama())).setParent(myvbox);
			new MyLabelAgakKecil("Kategori: " + (prestasiPegawai.getKategoriPrestasiPegawai() == null ? ""
					: prestasiPegawai.getKategoriPrestasiPegawai().getNama())).setParent(myvbox);
			new MyLabelAgakKecil("Jml Peserta: " + prestasiPegawai.getJumlahPeserta()).setParent(myvbox);
			new MyLabelAgakKecil("Link: " + prestasiPegawai.getUrl()).setParent(myvbox);

			new Label(prestasiPegawai.getCapaian()).setParent(arg0);

			new Label(prestasiPegawai.getNomorSertifikat()).setParent(arg0);

			// Kolom aksi rapi: semua tombol dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi.
			// aksiBoxRef menampung Vbox pembungkus supaya visibilitas grup tetap bisa
			// diubah dari listener Combobox status (perilaku sama dgn Hbox toolbar lama).
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			final Vbox[] aksiBoxRef = new Vbox[1];

			if (tbmuser != null && tbmuser.getPegawai() != null && prestasiPegawai.getPegawai() != null
					&& !prestasiPegawai.getPegawai().getId().equals(tbmuser.getPegawai().getId()) && (

					(punyaBawahan != null && punyaBawahan.contains(prestasiPegawai.getPegawai().getId())) ||

							(prestasiPegawai.getPegawai().getAtasanlangsung() != null && tbmuser.getPegawai().getId()
									.equals(prestasiPegawai.getPegawai().getAtasanlangsung().getId()))

							||

							(prestasiPegawai.getPegawai().getAtasanlangsung2() != null && tbmuser.getPegawai().getId()
									.equals(prestasiPegawai.getPegawai().getAtasanlangsung2().getId()))

							||

							(prestasiPegawai.getPegawai().getAtasanlangsung3() != null && tbmuser.getPegawai().getId()
									.equals(prestasiPegawai.getPegawai().getAtasanlangsung3().getId()))

					)

			) {
				final Combobox status = new Combobox();
				Comboitem comboitem = new Comboitem(PrestasiPegawai.BELUM_DIPROSES);
				comboitem.setValue(PrestasiPegawai.BELUM_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PrestasiPegawai.SEDANG_DIPROSES);
				comboitem.setValue(PrestasiPegawai.SEDANG_DIPROSES);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PrestasiPegawai.DISETUJUI);
				comboitem.setValue(PrestasiPegawai.DISETUJUI);
				status.appendChild(comboitem);

				comboitem = new Comboitem(PrestasiPegawai.DITOLAK);
				comboitem.setValue(PrestasiPegawai.DITOLAK);
				status.appendChild(comboitem);

				Common.selectComboItem(status, prestasiPegawai.getStatus());
				status.setParent(arg0);
				status.setReadonly(true);
				status.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						prestasiPegawai.setStatus((String) (status.getSelectedItem() == null
								|| status.getSelectedItem().getValue() == null ? null
										: status.getSelectedItem().getValue()));
						Common.refreshUpdate(prestasiPegawai);
						if (aksiBoxRef[0] != null) {
							aksiBoxRef[0].setVisible(!prestasiPegawai.getStatus().equals(PrestasiPegawai.DISETUJUI));
						}
					}
				};
				status.addEventListener("onChange", eventListener);
			} else {
				new Label(prestasiPegawai.getStatus()).setParent(arg0);
			}

			new Label(prestasiPegawai.getKeterangan()).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(prestasiPegawai);
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

											if (prestasiPegawaiSelected != null && prestasiPegawaiSelected.getId()
													.equals(prestasiPegawai.getId())) {
												prestasiPegawaiSelected = null;
											}

											Common.refreshDelete(prestasiPegawai);
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
			aksiBoxRef[0].setVisible(!prestasiPegawai.getStatus().equals(PrestasiPegawai.DISETUJUI) && tbmuser != null);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PrestasiPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final PrestasiPegawai prestasiPegawai) throws Exception {
		this.prestasiPegawai = prestasiPegawai;
		addWindow.setTitle(prestasiPegawai.getId() == null ? "Tambah Prestasi Pegawai" : "Ubah Prestasi Pegawai");
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
		row.appendChild(nama = new Textbox(prestasiPegawai.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kejuaraan (dalam bhs inggris)"));
		row.appendChild(namaEn = new Textbox(prestasiPegawai.getNamaEn()));
		namaEn.setWidth("90%");
		namaEn.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kejuaraan *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		tanggal = new MyDatebox(prestasiPegawai.getTanggal());
		hbox.appendChild(tanggal);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		tanggalSelesai = new MyDatebox(prestasiPegawai.getTanggalSelesai());
		hbox.appendChild(tanggalSelesai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());
		pegawai.setWidth("90%");
		pegawai.setReadonly(true);

		if (mhs != null) {
			pegawai.setAttribute("pegawai", mhs);
			pegawai.setDisabled(true);
			pegawai.setValue(mhs.getNama());
		} else {
			pegawai.setAttribute("pegawai", prestasiPegawai.getPegawai());
			pegawai.setValue(prestasiPegawai.getPegawai() == null ? "" : prestasiPegawai.getPegawai().getNama());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat Kejuaraan *"));
		row.appendChild(tempat = new Textbox(prestasiPegawai.getTempat()));
		tempat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Juara ke *"));
		row.appendChild(juara = new Textbox(prestasiPegawai.getJuara()));
		juara.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peringkat"));
		row.appendChild(peringkat = new Intbox(prestasiPegawai.getPeringkat()));
		peringkat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyelenggara *"));
		row.appendChild(penyelenggara = new Textbox(prestasiPegawai.getPenyelenggara()));
		penyelenggara.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Sertifikat Prestasi *"));
		row.appendChild(nomorSertifikat = new Textbox(prestasiPegawai.getNomorSertifikat()));
		nomorSertifikat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cabang *"));
		row.appendChild(cabangPrestasiPegawai = new Combobox());
		Common.insertCombo(cabangPrestasiPegawai, "nama", CabangPrestasiPegawai.class);
		Common.selectComboItem(cabangPrestasiPegawai, prestasiPegawai.getCabangPrestasiPegawai());
		cabangPrestasiPegawai.setWidth("90%");
		cabangPrestasiPegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori *"));
		row.appendChild(kategoriPrestasiPegawai = new Combobox());
		Common.insertCombo(kategoriPrestasiPegawai, "nama", KategoriPrestasiPegawai.class);
		Common.selectComboItem(kategoriPrestasiPegawai, prestasiPegawai.getKategoriPrestasiPegawai());
		kategoriPrestasiPegawai.setWidth("90%");
		kategoriPrestasiPegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Peserta *"));
		row.appendChild(jumlahPeserta = new Textbox(prestasiPegawai.getJumlahPeserta()));
		jumlahPeserta.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Capaian *"));
		row.appendChild(capaian = new Textbox(prestasiPegawai.getCapaian()));
		capaian.setWidth("90%");
		capaian.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link / URL"));
		row.appendChild(url = new Textbox(prestasiPegawai.getUrl()));
		url.setWidth("90%");
		url.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(tahun = new Intbox(prestasiPegawai.getTahun()));
		tahun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(prestasiPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Scan / foto sertifikat prestasi *"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, prestasiPegawai.getId(), PrestasiPegawai.class.getName(),
				"Lampiran Sertifikat", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainPegawai = (LampiranLain) arg0.getData();
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
		if (pegawai.getAttribute("pegawai") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Pegawai",
					"Kolom Pegawai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Pegawai.",
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

		if (cabangPrestasiPegawai.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Cabang Kejuaraan",
					"Kolom Cabang Kejuaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Cabang Kejuaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (kategoriPrestasiPegawai.getSelectedItem() == null) {
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

		try {

			if (prestasiPegawai != null && prestasiPegawai.getId() != null) {

				LampiranLain lam = LampiranLain.ambil(prestasiPegawai.getId(), PrestasiPegawai.class.getName());
				if (lam == null) {
					MyMessageboxConfig.show("File scan / foto sertifikat prestasi harus diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			} else {
				if (lainPegawai == null) {
					MyMessageboxConfig.show("File scan / foto sertifikat prestasi harus diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			}

		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/PrestasiPegawaiAction.java:968");
		}

		Session session = HibernateUtil.currentSession();
		if (prestasiPegawai.getId() != null) {
			prestasiPegawai = (PrestasiPegawai) session.load(PrestasiPegawai.class, prestasiPegawai.getId());

		}

		prestasiPegawai.setPeringkat(peringkat.getValue());
		prestasiPegawai.setCabangPrestasiPegawai(
				(CabangPrestasiPegawai) (cabangPrestasiPegawai.getSelectedItem() == null ? null
						: cabangPrestasiPegawai.getSelectedItem().getValue()));
		prestasiPegawai.setKategoriPrestasiPegawai(
				(KategoriPrestasiPegawai) (kategoriPrestasiPegawai.getSelectedItem() == null ? null
						: kategoriPrestasiPegawai.getSelectedItem().getValue()));
		prestasiPegawai.setJumlahPeserta(jumlahPeserta.getValue());
		prestasiPegawai.setCapaian(capaian.getValue());
		prestasiPegawai.setUrl(url.getValue());

		prestasiPegawai.setTanggal(tanggal.getValue());
		prestasiPegawai.setTanggalSelesai(tanggalSelesai.getValue());
		prestasiPegawai.setNama(nama.getValue());
		prestasiPegawai.setNamaEn(namaEn.getValue());
		prestasiPegawai.setTempat(tempat.getValue());
		prestasiPegawai.setJuara(juara.getValue());
		prestasiPegawai.setNomorSertifikat(nomorSertifikat.getValue());
		prestasiPegawai.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		prestasiPegawai.setKeterangan(keterangan.getValue());
		prestasiPegawai.setPenyelenggara(penyelenggara.getValue());
		prestasiPegawai.setTanggal(tanggal.getValue());

		Common.refreshSaveOrUpdate(session, prestasiPegawai);

		if (lainPegawai != null && lainPegawai.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainPegawai);
				lainPegawai.setRef(prestasiPegawai.getId());

				session.getTransaction().begin();
				session.update(lainPegawai);
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

		Long loginAtasan = tbmuser != null && tbmuser.ambilPegawai() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("pegawai") ? tbmuser.getPegawai().getId() : null;

		System.out.println("loginAtasan => " + loginAtasan);

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PrestasiPegawai.class)

				.createAlias("pegawai", "pegawai")

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("pegawai.satuanKerja", satuanKerjas))

				.add((searchpegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("pegawai", searchpegawai.getAttribute("pegawai")),
								Restrictions.eq("pegawai.atasanlangsung", loginAtasan))))

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

				.add(searchcabangPrestasiPegawai.getSelectedItem() == null
						|| searchcabangPrestasiPegawai.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("cabangPrestasiPegawai",
										searchcabangPrestasiPegawai.getSelectedItem().getValue()))

				.add(searchkategoriPrestasiPegawai.getSelectedItem() == null
						|| searchkategoriPrestasiPegawai.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kategoriPrestasiPegawai",
										searchkategoriPrestasiPegawai.getSelectedItem().getValue()))

		;

		if (punyaBawahan != null && !punyaBawahan.isEmpty() && punyaBawahanDosen != null
				&& !punyaBawahanDosen.isEmpty()) {
			criteria.createAlias("pegawai.dosen", "dosen", Criteria.LEFT_JOIN).add(Restrictions
					.or(Restrictions.in("pegawai.id", punyaBawahan), Restrictions.in("dosen.id", punyaBawahanDosen)));
		} else if (punyaBawahan != null && !punyaBawahan.isEmpty()) {
			criteria.add(Restrictions.in("pegawai.id", punyaBawahan));
		} else if (punyaBawahanDosen != null && !punyaBawahanDosen.isEmpty()) {
			criteria.createAlias("pegawai.dosen", "dosen", Criteria.LEFT_JOIN)
					.add(Restrictions.in("dosen.id", punyaBawahanDosen));
		} else {
			criteria.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
					: Restrictions.in("pegawai.satuanKerja", satuanKerjas));

		}

		System.out.println("punyaBawahan -> " + punyaBawahan);
		System.out.println("punyaBawahanDosen -> " + punyaBawahanDosen);

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PrestasiPegawai> myPrestasiPegawais;

		if (prestasiPegawaiSelected != null) {
			myPrestasiPegawais = new ArrayList<PrestasiPegawai>();
			myPrestasiPegawais.add(prestasiPegawaiSelected);
			myPrestasiPegawais.addAll(initCriteria(true).add(Restrictions.ne("id", prestasiPegawaiSelected.getId()))
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list());
		} else {
			myPrestasiPegawais = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		}

		ListModel strset = new SimpleListModel(myPrestasiPegawais);
		grid.setRowRenderer(new PrestasiPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
