package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.RencanaTahunAkademikAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.VOPembelajaran;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

public class RekapitulasiTugasHelper {

	public static void display(Component parent, final Tbmuser tbmuser) {
		display(parent, tbmuser, null);
	}

	public static void display(Component parent, final Tbmuser tbmuser, final VOPembelajaran perkuliahan) {

		org.zkoss.zul.Vbox subVboxUtama = new org.zkoss.zul.Vbox();
		subVboxUtama.setWidth("100%");
		subVboxUtama.setParent(parent);

		final org.zkoss.zul.Div center = new org.zkoss.zul.Div();
		center.setWidth("100%");
		Toolbar hbox = new Toolbar();
		hbox.setParent(subVboxUtama);

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
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiTugasHelper.java:116");
					// TODO: handle exception
				}
			}
		}, 3, "Tugas").setParent(hbox);

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

		// Tombol "Recovery" (permintaan user): buka riwayat SEMUA tugas (Envers) pada pembelajaran
		// (VoPembelajaran) yang SAMA + tombol KEMBALIKAN untuk memulihkan tugas yang terhapus/berubah.
		// Difilter ke perkuliahan / jadwalPelajaran sesuai VOPembelajaran. Hanya untuk pengelola
		// (dosen/admin), bukan mahasiswa/siswa.
		boolean bolehKelolaRec = tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null;
		if (bolehKelolaRec) {
			MyToolbarbuttonConfig recovery = new MyToolbarbuttonConfig("Recovery", "/img/jadwal.png");
			recovery.setTooltiptext(
					"Riwayat semua tugas pada pembelajaran ini — kembalikan/pulihkan tugas yang terhapus atau berubah.");
			recovery.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						String prop = null;
						Object val = null;
						if (perkuliahan instanceof Perkuliahan) {
							prop = "perkuliahan";
							val = perkuliahan;
						} else if (perkuliahan instanceof JadwalPelajaran) {
							prop = "jadwalPelajaran";
							val = perkuliahan;
						}
						RevisiTugasHelper rh = new RevisiTugasHelper(Pertemuan.class, prop, val, null);
						org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(rh);
						rh.setVisible(true);
						rh.onModal();
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			});
			recovery.setParent(hbox);
		}

		center.setParent(subVboxUtama);

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
	private static void reload(final Tbmuser tbmuser, final Component center, final Date mulai, final Date sampai,
			final String cari, boolean refreh, boolean awal, final VOPembelajaran perkuliahan) {
		if (center != null) {
			Common.clear(center);
		}

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
		column.setLabel("Tugas");

		reload(pertemuans, paging, grid, mahasiswa, awal, mulai, sampai, cari, perkuliahan);

	}

	@SuppressWarnings("unchecked")
	private static void reload(final java.util.List<Long> pertemuans, final org.zkoss.zul.Paging paging, final ais.ui.util.MyGrid grid,
			final ais.database.model.Mahasiswa mahasiswa, boolean awal, final java.util.Date mulai, final java.util.Date sampai, final String cari,
			final ais.database.model.VOPembelajaran perkuliahan) {

		org.hibernate.Session session = null;

		try {
			session = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();

			boolean usePerkuliahan = (perkuliahan != null);
			boolean useIn = (pertemuans != null && !pertemuans.isEmpty());
			boolean isAdmin = ais.common.Common.getApakahAdmin() && !usePerkuliahan;
			boolean hasCari = (cari != null && !cari.trim().isEmpty());

			// OPTIMASI SQL: Menyuntikkan klausa WHERE langsung ke dalam masing-masing tabel (Push-Down Predicate)
			StringBuilder whereP = new StringBuilder(" WHERE judultugas IS NOT NULL AND judultugas != '' ");
			StringBuilder whereT = new StringBuilder(" WHERE aa.judultugas IS NOT NULL AND aa.judultugas != '' ");

			if (!usePerkuliahan) {
				if (mulai != null && sampai != null) {
					// PERBAIKAN FATAL ERROR: Tambahkan CAST(... AS DATE) agar PostgreSQL tidak protes
					// perbandingan antara tipe DATE dan VARCHAR(String)
					whereP.append(" AND DATE(COALESCE(mulai, tanggal)) BETWEEN CAST(:mulai AS DATE) AND CAST(:sampai AS DATE) ");
					whereT.append(" AND DATE(COALESCE(aa.mulai, bb.tanggal)) BETWEEN CAST(:mulai AS DATE) AND CAST(:sampai AS DATE) ");
				}
			}

			if (isAdmin) {
				// Tidak ada tambahan filter
			} else if (!useIn) {
				whereP.append(" AND 1=0 ");
				whereT.append(" AND 1=0 ");
			} else {
				whereP.append(" AND id IN (:listPertemuan) ");
				whereT.append(" AND aa.pertemuan IN (:listPertemuan) ");
			}

			if (hasCari) {
				whereP.append(" AND judultugas ILIKE :cari ");
				whereT.append(" AND aa.judultugas ILIKE :cari ");
			}

			// Susun Core Query
			String sqlBaseData =
					"SELECT id, 'pertemuan' as jenis, COALESCE(mulai, tanggal) as mulai_real, selesai, judultugas, id as pertemuan_id " +
					"FROM pertemuan " + whereP.toString() +
					"UNION ALL " +
					"SELECT aa.id, 'tugas' as jenis, COALESCE(aa.mulai, bb.tanggal) as mulai_real, aa.selesai, aa.judultugas, aa.pertemuan as pertemuan_id " +
					"FROM tugas_pertemuan aa INNER JOIN pertemuan bb ON (aa.pertemuan = bb.id) " + whereT.toString();

			String sqlCount = "SELECT COUNT(*) FROM (" + sqlBaseData + ") AS count_tbl";

			org.hibernate.Query queryCount = session.createSQLQuery(sqlCount);

			// Inject Parameter
			if (!usePerkuliahan && mulai != null && sampai != null) {
				queryCount.setParameter("mulai", ais.common.Common.databaseDateFormat.get().format(mulai));
				queryCount.setParameter("sampai", ais.common.Common.databaseDateFormat.get().format(sampai));
			}
			if (!isAdmin && useIn) {
				queryCount.setParameterList("listPertemuan", pertemuans);
			}
			if (hasCari) {
				queryCount.setParameter("cari", "%" + cari.trim() + "%");
			}

			int size = ((Number) queryCount.uniqueResult()).intValue();
			paging.setTotalSize(size);
			paging.setVisible(size > ais.common.Common.ROWS_COUNT_ON_PAGE);

			if (awal) {
				String sqlCountAwal = "SELECT COUNT(*) FROM (" + sqlBaseData + ") AS count_tbl WHERE DATE(mulai_real) < CURRENT_DATE";
				org.hibernate.Query queryCountAwal = session.createSQLQuery(sqlCountAwal);

				if (!usePerkuliahan && mulai != null && sampai != null) {
					queryCountAwal.setParameter("mulai", ais.common.Common.databaseDateFormat.get().format(mulai));
					queryCountAwal.setParameter("sampai", ais.common.Common.databaseDateFormat.get().format(sampai));
				}
				if (!isAdmin && useIn) {
					queryCountAwal.setParameterList("listPertemuan", pertemuans);
				}
				if (hasCari) {
					queryCountAwal.setParameter("cari", "%" + cari.trim() + "%");
				}

				int page = ((Number) queryCountAwal.uniqueResult()).intValue();

				try {
					int activePage = (int) Math.floor((float) page / ais.common.Common.ROWS_COUNT_ON_PAGE);
					paging.setActivePage(activePage);
				} catch (Exception e) {
					try {
						paging.setActivePage(0);
					} catch (Exception ae) { ais.common.ErrorAuditUtil.record(ae, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiTugasHelper.java:422");}
				}
			}

			// Eksekusi Data Utama
			String sqlData = "SELECT * FROM (" + sqlBaseData + ") AS data_tbl ORDER BY mulai_real ASC, id ASC LIMIT :limit OFFSET :offset";
			org.hibernate.Query queryData = session.createSQLQuery(sqlData);

			if (!usePerkuliahan && mulai != null && sampai != null) {
				queryData.setParameter("mulai", ais.common.Common.databaseDateFormat.get().format(mulai));
				queryData.setParameter("sampai", ais.common.Common.databaseDateFormat.get().format(sampai));
			}
			if (!isAdmin && useIn) {
				queryData.setParameterList("listPertemuan", pertemuans);
			}
			if (hasCari) {
				queryData.setParameter("cari", "%" + cari.trim() + "%");
			}

			queryData.setParameter("limit", ais.common.Common.ROWS_COUNT_ON_PAGE);
			queryData.setParameter("offset", ais.common.Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()));

			java.util.List<Object[]> tugasPertemuans = queryData.list();

			org.zkoss.zul.ListModel strset = new org.zkoss.zul.SimpleListModel(tugasPertemuans);
			grid.setModelCheckMobile(strset);

			org.zkoss.zk.ui.event.EventListener eventListener = new org.zkoss.zk.ui.event.EventListener() {
				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
					reload(pertemuans, paging, grid, mahasiswa, false, mulai, sampai, cari, perkuliahan);
				}
			};

			grid.setRowRenderer(new DetailPertemuanRenderer(mahasiswa, null, eventListener));

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiTugasHelper.java:459");
		} finally {
			if (session != null) {
				try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiTugasHelper.java:462");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiTugasHelper.java:463");}
				closeHibernateSessionQuietly(session);
			}
		}
	}

	public static class DetailPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		private Mahasiswa mahasiswa;
		private BiodataCalonMahasiswa biodataCalonMahasiswa;
		private EventListener eventListener;

		public DetailPertemuanRenderer(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
				EventListener eventListener) {
			this.mahasiswa = mahasiswa;
			this.biodataCalonMahasiswa = biodataCalonMahasiswa;
			this.eventListener = eventListener;
		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			Object[] objects = (Object[]) data;
			Session session = HibernateUtil.currentSession();
			Long id = ((Number) objects[0]).longValue();
			String jenis = objects[1] + "";

			final TugasPertemuan tugasPertemuan = jenis.equalsIgnoreCase("tugas")
					? (TugasPertemuan) session.createCriteria(TugasPertemuan.class).add(Restrictions.idEq(id))
							.uniqueResult()
					: null;

			final Pertemuan pertemuan = jenis
					.equalsIgnoreCase("pertemuan")
							? (Pertemuan) session.createCriteria(Pertemuan.class).add(Restrictions.idEq(id))
									.uniqueResult()
							: (tugasPertemuan != null && tugasPertemuan.getPertemuan() != null
									? (Pertemuan) session.createCriteria(Pertemuan.class)
											.add(Restrictions.idEq(tugasPertemuan.getPertemuan())).uniqueResult()
									: null);
			if (pertemuan == null) {
				arg0.setVisible(false);
				return;
			}
			final Tugas tugas = (Tugas) (tugasPertemuan != null ? tugasPertemuan : pertemuan);
			if (tugas.getJudultugas() != null && !tugas.getJudultugas().trim().isEmpty()) {
				String n = tugas.getJudultugas();
				if (n == null) {
					n = "";
				}
				Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
				vbox.setWidth("90%");
				vbox.setParent(arg0);

				String icon = "/img/svg/list-task.svg";
				if (tugas instanceof TugasKelompok) {
					icon = "/img/svg/user-group.svg";
				}

				Toolbarbutton downloadButton = new ais.ui.util.MyToolbarbuttonConfig(
						n.length() > 150 ? n.substring(0, 150) + "..." : n, icon);

				downloadButton.setAttribute("janganDisabled", true);
				vbox.appendChild(downloadButton);
				downloadButton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (tugas instanceof Pertemuan) {
							new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, new DataLoader() {

								@Override
								public void loadData(Object value) {
									try {
										eventListener.onEvent(null);
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiTugasHelper.java:540");
									}
								}
							}, 3);
						} else if (tugas instanceof TugasKelompok) {
							TugasKelompok tugasKelompok = (TugasKelompok) tugas;
							new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, new DataLoader() {

								@Override
								public void loadData(Object value) {
									try {
										eventListener.onEvent(null);
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiTugasHelper.java:554");
									}

								}
							}, 3, null, tugasKelompok, null, null, null);
						} else {
							TugasPertemuan tugasPertemuan = (TugasPertemuan) tugas;
							new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, new DataLoader() {

								@Override
								public void loadData(Object value) {
									try {
										eventListener.onEvent(null);
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiTugasHelper.java:569");
									}
								}
							}, 3, tugasPertemuan, null, null, null, null);
						}

					}
				});

				Date tgl = tugas.getMulai() == null ? pertemuan.getTanggal() : tugas.getMulai();
				Date tgl1 = tugas.getSelesai();

				Hbox myHbox = new Hbox();
				myHbox.setParent(vbox);

				if (tugas instanceof TugasKelompok) {

				} else if (tugas.getJudultugas() != null && !tugas.getJudultugas().trim().equals("")) {
					Number tg = tugas.ambilJumlahTugasFileContent();
					MyLabelKecil labelKecil = new MyLabelKecil(
							"Upload : " + Common.numberFormat.get().format(tg.intValue()) + " peserta");
					labelKecil.setStyle("font-size:8px;color:blue;");
					myHbox.appendChild(labelKecil);
				}
				TampilanELearningAction.dilihat(tugas, "tugas", "Akses", false).setParent(myHbox);

				A a;
				vbox.appendChild(
						a = new A(
								"Tugas pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info() + ", "
										+ (SmartDateTimeUtil.getDayString(tgl, null) + Common.dateFormat51.get().format(tgl))
										+ (tgl1 == null ? ""
												: " sampai dengan " + (SmartDateTimeUtil.getDayString(tgl1, null)
														+ Common.dateFormat51.get().format(tgl1)))
										+ ", " + pertemuan.getTopik()));

				a.setStyle("font-size:10px;");

				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (tugas instanceof Pertemuan) {
							new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, new DataLoader() {

								@Override
								public void loadData(Object value) {
									try {
										eventListener.onEvent(null);
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiTugasHelper.java:621");
									}

								}
							}, 3);
						} else if (tugas instanceof TugasKelompok) {
							TugasKelompok tugasKelompok = (TugasKelompok) tugas;
							new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, new DataLoader() {

								@Override
								public void loadData(Object value) {
									try {
										eventListener.onEvent(null);
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiTugasHelper.java:636");
									}
								}
							}, 3, null, tugasKelompok, null, null, null);
						} else {
							TugasPertemuan tugasPertemuan = (TugasPertemuan) tugas;
							new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, new DataLoader() {

								@Override
								public void loadData(Object value) {
									try {
										eventListener.onEvent(null);
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiTugasHelper.java:650");
									}

								}
							}, 3, tugasPertemuan, null, null, null, null);
						}

					}
				});
			}

		}

	}


	private static void closeHibernateSessionQuietly(Session session) {
		ais.common.ElearningSessionUtil.closeQuietly(session);
	}

}
