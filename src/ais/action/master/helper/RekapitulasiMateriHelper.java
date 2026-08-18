package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.RencanaTahunAkademikAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.profile.ProfileUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFoto;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMenuitem;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

public class RekapitulasiMateriHelper {

	public static void display(Component parent, final Tbmuser tbmuser) {
		display(parent, tbmuser, null);
	}

	public static void display(Component parent, final Tbmuser tbmuser, final VOPembelajaran perkuliahan) {

		Borderlayout subBorderlayoutUtama = new Borderlayout();
		subBorderlayoutUtama.setParent(parent);

		final Center center = new Center();
		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(subBorderlayoutUtama);
		north.setHeight("38px");
		Toolbar hbox = new Toolbar();
		hbox.setParent(north);

		final Textbox cari = new Textbox();
		hbox.appendChild(new MyLabelConfig("Cari:"));
		hbox.appendChild(cari);
		cari.setCols(10);

		RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction
				.getCurrentRencanaTahunAkademik(WaktuUtil.getDate());

		Calendar calendarMulai = Calendar.getInstance();
		calendarMulai
				.setTime(rencanaTahunAkademik == null ? WaktuUtil.getDate() : rencanaTahunAkademik.getTanggalMulai());
		calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 1);

		Calendar calendarSampai = Calendar.getInstance();
		calendarSampai
				.setTime(rencanaTahunAkademik == null ? WaktuUtil.getDate() : rencanaTahunAkademik.getTanggalSampai());
		calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 1);

		final MyDatebox mulai = new MyDatebox(calendarMulai.getTime());
		mulai.setReadonly(true);

		final MyDatebox sampai = new MyDatebox(calendarSampai.getTime());
		sampai.setReadonly(true);

		if (perkuliahan == null) {
			hbox.appendChild(new MyLabelConfig("Tanggal"));
			hbox.appendChild(mulai);
			hbox.appendChild(new MyLabelConfig("sd"));
			hbox.appendChild(sampai);
		}

		RekapitulasiUjianHelper.buatbaru(tbmuser, perkuliahan, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					reload(tbmuser, center, mulai.getValue(), sampai.getValue(), cari.getValue().trim(), true, true,
							perkuliahan);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiMateriHelper.java:126");
					// TODO: handle exception
				}
			}
		}, 2, "Materi").setParent(hbox);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
		refresh.setTooltiptext("Refresh");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload(tbmuser, center, mulai.getValue(), sampai.getValue(), cari.getValue().trim(), true, true,
						perkuliahan);
			}
		});
		refresh.setParent(hbox);

		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(subBorderlayoutUtama);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload(tbmuser, center, mulai.getValue(), sampai.getValue(), cari.getValue().trim(), false, true,
						perkuliahan);
			}
		};

		Common.createDefaultTimer(eventListener);
		cari.addEventListener("onOK", eventListener);
		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);
	}

	@SuppressWarnings("unchecked")
	private static void reload(final Tbmuser tbmuser, final Center center, final Date mulai, final Date sampai,
			final String cari, boolean refreh, boolean awal, final VOPembelajaran perkuliahan) {
		Common.clear(center);

		final Mahasiswa mahasiswa = tbmuser.getMahasiswa();
		Dosen dosen = tbmuser.ambilDosen();

		final Paging paging = new Paging();

		final List<Long> pertemuans = new ArrayList<Long>();
		Session session = HibernateUtil.currentSession();
		if (perkuliahan != null) {
			Criteria criteria = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(perkuliahan instanceof Perkuliahan ? Restrictions.eq("perkuliahan", perkuliahan)
							: perkuliahan instanceof JadwalPelajaran ? Restrictions.eq("jadwalPelajaran", perkuliahan)
									: Restrictions.sqlRestriction("false"));
			pertemuans.addAll(criteria.setProjection(Projections.property("id")).setMaxResults(32766).list());
		}

		else if (mahasiswa != null) {

			if (refreh) {

				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.setTime(mulai);
				calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 6);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.setTime(sampai);
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 6);

				mahasiswa.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
			}

			pertemuans.addAll(mahasiswa.ambilPertemuan(session).values());

		} else if (dosen != null) {

			if (refreh) {

				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.setTime(mulai);
				calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 6);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.setTime(sampai);
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 6);

				dosen.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
			}

			pertemuans.addAll(dosen.ambilPertemuan(session).values());
		} else if (!Common.getApakahAdmin()) {

			Sekolah sk = SekolahUtil.getSekolah();
			Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
			Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

			if (guru != null) {
				sk = guru.getSekolah();
			}
			if (siswa != null) {
				sk = siswa.getSekolah();
			}

			Calendar calendarMulai = Calendar.getInstance();
			calendarMulai.setTime(mulai);
			calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 6);

			Calendar calendarSampai = Calendar.getInstance();
			calendarSampai.setTime(sampai);
			calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 6);

			String mul = null;
			String sam = null;

			Integer bln = 1;
			Integer ke = null;

			boolean jadwalPerkuliahan = !(sk != null && sk.getId() != null);

			boolean jadwalPelajaran = (sk != null && sk.getId() != null);

			boolean jadwalKkn = jadwalPerkuliahan;

			boolean jadwalPkl = jadwalPerkuliahan;

			boolean jadwalKegiatan = jadwalPerkuliahan;

			boolean jadwalRevisi = jadwalPerkuliahan;
			boolean jadwalKonsultasi = jadwalPerkuliahan;

			boolean jadwalBimbingan = jadwalPerkuliahan;

			boolean jadwalKonsultasiLain = jadwalPerkuliahan;

			boolean tdpDiskusi = false;

			boolean tdpUjian = false;

			boolean tdpMateri = false;
			boolean tdpTugas = false;
			boolean tdpCatatan = false;
			boolean tdpAudio = false;
			boolean tdpVideo = false;
			boolean tdpDosenPengganti = false;

			String cariMk = "";
			String cariDosen = "";
			String cariTopik = "";
			String cariCatatan = "";
			String cariMahasiswa = "";
			String cariKelas = "";
			String cariRuang = "";

			Integer ekstra = null;

			boolean remedial = false;
			boolean paralel = false;
			boolean pra = false;
			StatusPertemuan statusPertemuan = null;
			boolean ujian = false;

			String day = null;

			Criteria criteria = DashboardTimelinePertemuan.initStaticCriteria(true, calendarMulai.getTime(),
					calendarSampai.getTime(), tbmuser, cari, mul, sam, bln, statusPertemuan, ke, day, tdpVideo,
					tdpAudio, tdpMateri, jadwalPerkuliahan, jadwalPelajaran, jadwalKkn, jadwalPkl, jadwalRevisi,
					jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain, jadwalKegiatan, tdpUjian, tdpDiskusi,
					tdpTugas, tdpCatatan, tdpDosenPengganti, cariTopik, cariCatatan, paralel, pra, remedial, cariMk,
					ekstra != null && ekstra.equals(Perkuliahan.EKSTRA), cariKelas, cariRuang, cariDosen, cariMahasiswa,
					ujian, sk, session);

			pertemuans.addAll(criteria.setProjection(Projections.property("id")).setMaxResults(32766).list());

		}

		paging.setDetailed(true);
		paging.setPageSize(Common.ROWS_COUNT_ON_PAGE);
		paging.setPageIncrement(Common.isMobile() ? 5 : 10);
		paging.setMold("os");

		final MyGrid grid = new MyGrid();
		paging.addEventListener("onPaging", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				reload(pertemuans, paging, grid, mahasiswa, false, mulai, sampai, cari, perkuliahan);
			}
		});

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(center);

		groupbox.appendChild(paging);

		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Materi");

		reload(pertemuans, paging, grid, mahasiswa, awal, mulai, sampai, cari, perkuliahan);

	}

	@SuppressWarnings("unchecked")
	private static void reload(final List<Long> pertemuans, final Paging paging, final MyGrid grid,
			final Mahasiswa mahasiswa, boolean awal, final Date mulai, final Date sampai, final String cari,
			final VOPembelajaran perkuliahan) {
		Session session = StreamingHibernateUtil.getInstance().currentSession();

		String inPer = "";
		for (Long id : pertemuans) {
			inPer += inPer.isEmpty() ? id.toString() : "," + id;
		}

		String where = "1=1";
		if (Common.getApakahAdmin() && perkuliahan == null) {
			where = where + " and true ";
		} else if (inPer.isEmpty()) {
			where = where + " and false ";
		} else {
			where = where + " and pertemuan in (" + inPer + ") ";

		}

		if (!cari.isEmpty()) {
			where = where + " and real_file ilike '%" + cari + "%' ";
		}

		String sql = "select count(*) as size from pertemuan_file_content a where " + where + ";";

		int size = ((Number) session.createSQLQuery(sql).uniqueResult()).intValue();
		paging.setTotalSize(size);
		paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);

		if (awal) {

			Long pointPertemuan = (Long) HibernateUtil.currentSession().createCriteria(Pertemuan.class)
					.setProjection(Projections.property("id"))
					.add(Restrictions.sqlRestriction("date(tanggal) < CURRENT_DATE"))
					.add(pertemuans.isEmpty() ? Restrictions.sqlRestriction("false")
							: Restrictions.in("id", pertemuans))
					.addOrder(Order.desc("tanggal")).setMaxResults(1).uniqueResult();
			if (pointPertemuan == null) {
				pointPertemuan = -1L;
			}

			String where1 = where + " and pertemuan < " + pointPertemuan;

			sql = "select count(*) as size from pertemuan_file_content a where " + where1 + ";";

			int page = ((Number) session.createSQLQuery(sql).uniqueResult()).intValue();

			try {
				System.out.println("page -> " + page + ", size -> " + size + ", pertemuans -> " + pertemuans.size());
				paging.setActivePage((int) (page / Common.ROWS_COUNT_ON_PAGE));
			} catch (Exception e) {
				try {

					paging.setActivePage(((int) (page / Common.ROWS_COUNT_ON_PAGE)) - 1);
				} catch (Exception ae) { ais.common.ErrorAuditUtil.record(ae, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiMateriHelper.java:392");
					// TODO: handle exception
				}
			}
		}

		List<PertemuanFileContent> tugasPertemuans = session.createCriteria(PertemuanFileContent.class)
				.add(Restrictions.sqlRestriction(where)).addOrder(Order.asc("pertemuan")).addOrder(Order.asc("id"))
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(tugasPertemuans);
		grid.setModelCheckMobile(strset);

		grid.setRowRenderer(new DetailPertemuanRenderer(mahasiswa, null));

		StreamingHibernateUtil.getInstance().closeSession();
	}

	public static class DetailPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		private Mahasiswa mahasiswa;
		private BiodataCalonMahasiswa biodataCalonMahasiswa;

		public DetailPertemuanRenderer(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) {
			this.mahasiswa = mahasiswa;
			this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) data;
			Session session = HibernateUtil.currentSession();
			final Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
					.add(Restrictions.idEq(pertemuanFileContent.getPertemuan())).uniqueResult();
			if (pertemuan == null) {
				arg0.setVisible(false);
				return;
			}

			String n = pertemuanFileContent.getNama() != null
					&& pertemuanFileContent.getNama().trim().equalsIgnoreCase("link") ? pertemuanFileContent.getLink()
							: pertemuanFileContent.getNama();

			if (!pertemuanFileContent.getGoogleBook().isEmpty()) {
				n = pertemuanFileContent.getNama();
			}
			if (n == null) {
				n = "";
			}

			String isi = pertemuanFileContent.getKeterangan();
			if (isi != null && !isi.trim().isEmpty()) {
				n = isi;
			}

			Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
			vbox.setWidth("90%");
			vbox.setParent(arg0);

			String icon = pertemuanFileContent.getLokasiFisik() != null
					? "/img/svg/desktop-light.svg"
					: MyMenuitem.svgIcon(pertemuanFileContent.getNama(),
					FileFoto.icon(pertemuanFileContent.getNama()));

			Toolbarbutton downloadButton = new ais.ui.util.MyToolbarbuttonConfig(n.length() > 150 ? n.substring(0, 150) + "..." : n,
					!pertemuanFileContent.getGoogleBook().isEmpty() ? "/img/Apps-Google-Play-Books-icon.png" : icon);
			downloadButton.setTooltiptext("Download \"" + pertemuanFileContent.getNama() + "\"");
			downloadButton.setAttribute("janganDisabled", true);
			vbox.appendChild(downloadButton);

			TampilanELearningAction
					.dilihat(pertemuan, "bahan_perkulaiahan_" + pertemuanFileContent.getId(), "Akses", false)
					.setParent(vbox);

			downloadButton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (ProfileUtil.chekSyarat(pertemuan.ambilVOPembelajaran(),
							pertemuanFileContent.getSyaratAkses())) {
						return;
					}

					pertemuan.masukkanData("bahan_perkulaiahan_" + pertemuanFileContent.getId());

					if (!pertemuanFileContent.getGoogleBook().isEmpty()) {
						if (Common.isMobile()) {
							ExecutionsCtrl.getCurrent().sendRedirect(pertemuanFileContent.getLink(), "_blank");
						} else {
							Clients.evalJavaScript("popupCenter({url: '" + pertemuanFileContent.getLink()
									+ "', title: 'Book', w: 1200, h: 600});");
						}
					} else if (pertemuanFileContent.getGdrive() != null) {
						pertemuanFileContent.tampilGDrive(null);
					} else {

						String link = pertemuanFileContent == null ? null
								: (pertemuanFileContent.getLink() == null || pertemuanFileContent.getLink().isEmpty()
										? null
										: pertemuanFileContent.getLink());

						if (pertemuanFileContent != null
								&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
							link = pertemuanFileContent.createLinkUri();
							if (link != null) {
								// link = link.replaceAll("download=false", "download=true");
							}
						}

						if (pertemuanFileContent != null && link != null && !link.trim().isEmpty()) {

							if (pertemuanFileContent.bisaPreview()) {
								Common.displayWindow(pertemuanFileContent.merupakanGambar(), link, true, "95%", "95%",
										true, pertemuanFileContent);
							} else {
								if (Common.isMobile()) {
									ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
								} else {
									Clients.evalJavaScript(
											"popupCenter({url: '" + link + "', title: 'data', w: 1200, h: 600});");
								}
							}
						} else {
							MyMessageboxConfig.show(
									"Mohon maaf, Bapak/Ibu, berkas yang Anda akses tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba akses kembali; (3) apabila kendala masih berlanjut, hubungi Admin atau bagian terkait untuk bantuan lebih lanjut.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}
				}
			});

			A a;
			vbox.appendChild(a = new A("Materi pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info()
					+ ", "
					+ ((pertemuan.getTanggal() == null ? "-"
							: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai())
									+ Common.dateFormat6.get().format(pertemuan.getTanggal()))
									+ " "
									+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
											: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai())))
					+ ", " + pertemuan.getTopik() + (pertemuanFileContent.getKeterangan().isEmpty() ? ""
							: ", " + pertemuanFileContent.getKeterangan())));

			a.setStyle("font-size:10px;");

			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, new DataLoader() {

						@Override
						public void loadData(Object value) {
							// TODO Auto-generated
							// method stub

						}
					}, 2, pertemuanFileContent);

				}
			});
		}

	}

}
