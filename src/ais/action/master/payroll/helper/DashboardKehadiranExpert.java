package ais.action.master.payroll.helper;

/*
 * Refactor dari method initDashboardKehadiran menjadi class terpisah.
 *
 * Cara pakai di class lama:
 *
 * private void initDashboardKehadiran(final Date filterMulai, final Date filterSampai,
 *         final List<Integer> filterHariAktif,
 *         final ais.database.model.rab.SatuanKerja filterSatker,
 *         final String sortBy) throws Exception {
 *     new DashboardKehadiranExpert(tabDashboardPanel, tbmuser, new DashboardKehadiranExpert.ReloadHandler() {
 *         public void reload(Date mulai, Date sampai, List<Integer> hariAktif,
 *                 ais.database.model.rab.SatuanKerja satker, String sortBy) throws Exception {
 *             initDashboardKehadiran(mulai, sampai, hariAktif, satker, sortBy);
 *         }
 *     }).render(filterMulai, filterSampai, filterHariAktif, filterSatker, sortBy);
 * }
 *
 * Catatan:
 * - Sesuaikan package/import sesuai lokasi class Anda.
 * - Tipe tbmuser di constructor saya tulis Object agar mudah ditempel. Jika ingin lebih strict,
 *   ganti Object dengan tipe user/session Anda.
 */

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

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.CommonDashboardHtmlHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CutiBersama;
import ais.database.model.Pegawai;
import ais.database.model.PengajuanPegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

public class DashboardKehadiranExpert {

	public interface ReloadHandler {
		void reload(Date mulai, Date sampai, List<Integer> hariAktif, ais.database.model.rab.SatuanKerja satker,
				String sortBy) throws Exception;
	}

	private final Component tabDashboardPanel;
	private final Object tbmuser;
	private final ReloadHandler reloadHandler;

	private Date dateMulai;
	private Date dateSampai;
	private String currentSort;
	private String currentKeyword;
	private List<Integer> activeDays;
	private ais.database.model.rab.SatuanKerja filterSatker;

	private MyCheckboxConfig[] chkHaris;
	private ais.ui.util.MyDatebox dbMulai;
	private ais.ui.util.MyDatebox dbSampai;
	private ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox cbSatker;
	private org.zkoss.zul.Textbox txtCariPegawai;

	private PortalRefs portal;
	private DashboardData data;

	public DashboardKehadiranExpert(Component tabDashboardPanel, Object tbmuser, ReloadHandler reloadHandler) {
		this.tabDashboardPanel = tabDashboardPanel;
		this.tbmuser = tbmuser;
		this.reloadHandler = reloadHandler;
	}

	public void render(final Date filterMulai, final Date filterSampai, final List<Integer> filterHariAktif,
			final ais.database.model.rab.SatuanKerja filterSatker, final String sortBy, final boolean muatData)
			throws Exception {
		if (tabDashboardPanel == null) {
			return;
		}

		Common.clear(tabDashboardPanel);
		this.filterSatker = filterSatker;
		prepareFilterState(filterMulai, filterSampai, filterHariAktif, sortBy);

		renderFilterPanel();

		if (!muatData) {
			// On-demand: jangan auto-hitung dashboard (berat & memblokir desktop ZK
			// sehingga tab lain tak bisa diklik). Cukup tampilkan ajakan; data baru
			// dihitung saat tombol "Tampilkan Analisis" ditekan.
			tampilkanAjakanMuatDashboard();
			return;
		}

		final org.zkoss.zul.Vbox loadingBox = tampilkanLoadingDashboardKehadiran(
				"Menyiapkan parameter dan filter dashboard kehadiran...", 5);

		final EventListener[] listener = new EventListener[1];
		listener[0] = new EventListener() {
			public void onEvent(Event event) throws Exception {
				try {
					tabDashboardPanel.removeEventListener("onLoadDashboardKehadiranExpert", listener[0]);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:114");
				}

				try {
					ProgressHandler progress = new ProgressHandler() {
						public void update(String message, int percent) {
							updateLoadingDashboardKehadiran(loadingBox, message, percent);
						}
					};

					progress.update("Membaca konfigurasi, unit kerja, dan hak akses pengguna...", 8);
					data = loadAndCalculateData(progress);
					data.progressHandler = progress;
					progress.update("Mengurutkan rincian log kehadiran...", 82);
					sortDailyLogs();
					progress.update("Membuat kerangka tampilan dashboard...", 86);
					portal = createPortalLayout();
					progress.update("Merender dashboard utama dan dashboard tambahan HRD...", 88);
					renderDashboards();
					progress.update("Dashboard kehadiran selesai ditampilkan.", 100);
					hapusLoadingDashboardKehadiranNanti(loadingBox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					tampilkanErrorLoadingDashboardKehadiran(loadingBox, e);
				}
			}
		};

		tabDashboardPanel.addEventListener("onLoadDashboardKehadiranExpert", listener[0]);
		org.zkoss.zk.ui.event.Events.echoEvent("onLoadDashboardKehadiranExpert", tabDashboardPanel, null);
	}

	/** Ajakan ramah saat dashboard belum dimuat (mode on-demand). Tab lain tetap responsif. */
	private void tampilkanAjakanMuatDashboard() {
		if (tabDashboardPanel == null) {
			return;
		}
		org.zkoss.zul.Vbox box = new org.zkoss.zul.Vbox();
		box.setWidth("100%");
		org.zkoss.zul.Html h = new org.zkoss.zul.Html(
				"<div style='padding:24px 18px;text-align:center;color:#475569;background:#f8fafc;"
						+ "border:1px dashed #cbd5e1;border-radius:12px;margin-top:6px;'>"
						+ "<div style='font-size:32px;line-height:1;margin-bottom:8px;'>&#128202;</div>"
						+ "<div style='font-size:15px;font-weight:800;color:#0f172a;'>Dashboard belum dimuat</div>"
						+ "<div style='font-size:12px;margin-top:6px;line-height:1.6;'>Atur rentang tanggal &amp; filter di atas, "
						+ "lalu klik tombol <b>&ldquo;Tampilkan Analisis&rdquo;</b> untuk memuat ringkasan kehadiran.<br/>"
						+ "Tab lain (Rekap Presensi, Per Pegawai, dll) bisa langsung dibuka tanpa menunggu.</div></div>");
		box.appendChild(h);
		tabDashboardPanel.appendChild(box);
	}

	private org.zkoss.zul.Vbox tampilkanLoadingDashboardKehadiran(String pesan, int persen) {
		if (tabDashboardPanel == null) {
			return null;
		}
		org.zkoss.zul.Vbox containerDasborGrid = new org.zkoss.zul.Vbox();
		containerDasborGrid.setWidth("100%");
		containerDasborGrid.setStyle("margin-bottom:12px;");
		org.zkoss.zul.Html htmlLoading = new org.zkoss.zul.Html(buildLoadingHtml(pesan, persen, null));
		containerDasborGrid.setAttribute("htmlLoading", htmlLoading);
		containerDasborGrid.appendChild(htmlLoading);
		tabDashboardPanel.appendChild(containerDasborGrid);
		return containerDasborGrid;
	}

	private void updateLoadingDashboardKehadiran(org.zkoss.zk.ui.Component loadingBox, String pesan, int persen) {
		if (loadingBox == null) {
			return;
		}
		Object htmlObj = loadingBox.getAttribute("htmlLoading");
		if (htmlObj instanceof org.zkoss.zul.Html) {
			((org.zkoss.zul.Html) htmlObj).setContent(buildLoadingHtml(pesan, persen, null));
		}
	}

	private void tampilkanErrorLoadingDashboardKehadiran(org.zkoss.zk.ui.Component loadingBox, Exception e) {
		if (loadingBox == null) {
			return;
		}
		Object htmlObj = loadingBox.getAttribute("htmlLoading");
		if (htmlObj instanceof org.zkoss.zul.Html) {
			((org.zkoss.zul.Html) htmlObj)
					.setContent(CommonDashboardHtmlHelper.errorState("Gagal memuat dashboard kehadiran.", e));
		}
	}

	private void hapusLoadingDashboardKehadiran(org.zkoss.zk.ui.Component loadingBox) {
		try {
			if (loadingBox != null && loadingBox.getParent() != null) {
				loadingBox.detach();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:205");
		}
	}

	private void hapusLoadingDashboardKehadiranNanti(final org.zkoss.zk.ui.Component loadingBox) {
		try {
			if (loadingBox == null || loadingBox.getParent() == null) {
				return;
			}
			org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer();
			timer.setDelay(1200);
			timer.setRepeats(false);
			timer.addEventListener("onTimer", new EventListener() {
				public void onEvent(Event event) throws Exception {
					hapusLoadingDashboardKehadiran(loadingBox);
				}
			});
			timer.setParent(loadingBox);
		} catch (Exception e) {
			hapusLoadingDashboardKehadiran(loadingBox);
		}
	}

	private String buildLoadingHtml(String pesan, int persen, String detailError) {
		return CommonDashboardHtmlHelper.progressBar(persen, pesan,
				detailError == null
						? "Mohon tunggu, sistem sedang mengambil data presensi, cuti/izin, pengajuan, lembur, dan menyiapkan grafik."
						: detailError);
	}

	private String escapeHtml(String text) {
		return CommonDashboardHtmlHelper.escape(text);
	}

	/**
	 * Di sinilah urutan dasbor diatur. Jika ingin mengubah urutan, cukup pindahkan
	 * pemanggilan method di bawah ini.
	 */
	private void renderDashboards() throws Exception {
		// Urutan utama sesuai kebutuhan HRD/pimpinan: dari paling atas sampai bawah.
		updateProgress("Merender Overview Presensi...", 88);
		renderOverviewPresensi();
		updateProgress("Merender Daftar Perhatian Khusus...", 89);
		renderWatchlistPerhatianKhusus();
		updateProgress("Merender Ringkasan Riwayat Kehadiran Karyawan...", 90);
		renderRingkasanRiwayatKehadiran();
		updateProgress("Merender Rincian Log Riwayat Kehadiran Pegawai...", 91);
		renderRincianLogRiwayatKehadiran();
		updateProgress("Merender Kinerja Kedisiplinan per Satuan Kerja...", 92);
		renderKinerjaKedisiplinanSatker();
		updateProgress("Merender Beban Lembur Pegawai...", 93);
		renderBebanLemburPegawai();
		updateProgress("Merender Grafik Distribusi Alasan Ketidakhadiran...", 94);
		renderGrafikDistribusiAlasanKetidakhadiran();
		updateProgress("Merender Grafik Trend Beban Kerja Harian...", 95);
		renderGrafikTrendBebanKerjaHarian();
		updateProgress("Merender Grafik Analisa dan Rekapitulasi Kondisi Kehadiran...", 96);
		renderGrafikAnalisaDetailKehadiran();

		// Dasbor tambahan HRD/pimpinan diletakkan setelah 10 dasbor utama.
		updateProgress("Merender dashboard tambahan HRD/Pimpinan...", 97);
		renderPeringkatPegawaiTerajin();
		renderTopPegawaiPalingDisiplin();
		renderPegawaiTidakAbsenPulang();
		renderStatistikProduktivitasKehadiran();
		renderRadarKesehatanKehadiranHtml();
		new DashboardKehadiranTambahan(portal.pcBottom, data).renderAll();
	}

	private void updateProgress(String pesan, int persen) {
		try {
			if (data != null && data.progressHandler != null) {
				data.progressHandler.update(pesan, persen);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:279");
		}
	}

	private void prepareFilterState(Date filterMulai, Date filterSampai, List<Integer> filterHariAktif, String sortBy)
			throws Exception {
		int tanggalMulaiAbsensi = 1;
		try {
			tanggalMulaiAbsensi = Integer.parseInt(Common.getKonfigurasi("tanggal_mulai_absensi", "1").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:288");
		}

		Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
		this.dateMulai = filterMulai;
		if (this.dateMulai == null) {
			calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) - 1);
			calendarUtama.set(Calendar.DATE, tanggalMulaiAbsensi);
			this.dateMulai = calendarUtama.getTime();
		}

		this.dateSampai = filterSampai;
		if (this.dateSampai == null) {
			calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) + 1);
			calendarUtama.set(Calendar.DATE, calendarUtama.get(Calendar.DATE) - 1);
			this.dateSampai = calendarUtama.getTime();
		}

		this.currentSort = (sortBy == null || sortBy.isEmpty()) ? "waktu_desc" : sortBy;
		this.currentKeyword = (String) tabDashboardPanel.getAttribute("keywordCari");
		this.activeDays = new java.util.ArrayList<Integer>();

		if (filterHariAktif != null) {
			this.activeDays.addAll(filterHariAktif);
		} else {
			String hariDefaultTidakAktif = Common.getKonfigurasi("hari_default_tidak_aktif", ",1,7,").getNilai();
			int hIdx = 1;
			for (@SuppressWarnings("unused")
			String h : Common.haris) {
				if (!hariDefaultTidakAktif.contains("," + hIdx + ",")) {
					this.activeDays.add(hIdx);
				}
				hIdx++;
			}
		}
	}

	private void renderFilterPanel() throws Exception {
		org.zkoss.zul.Div filterContainer = new org.zkoss.zul.Div();
		filterContainer.setStyle(
				"padding: 15px; background: #ffffff; border-radius: 12px; border: 1px solid #e9ecef; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.02);");
		filterContainer.setParent(tabDashboardPanel);

		org.zkoss.zul.Grid filterGrid = new org.zkoss.zul.Grid();
		filterGrid.setSclass("fgrid");
		filterGrid.setStyle("border: none; background: transparent;");
		filterGrid.setParent(filterContainer);

		org.zkoss.zul.Rows filterRows = new org.zkoss.zul.Rows();
		filterRows.setParent(filterGrid);

		org.zkoss.zul.Row row1 = new org.zkoss.zul.Row();
		row1.setStyle("background: transparent; border: none;");
		row1.setParent(filterRows);

		org.zkoss.zul.Hbox rangeBox = new org.zkoss.zul.Hbox();
		rangeBox.setStyle("align-items: center; gap: 8px;");
		rangeBox.appendChild(new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Mulai:")));
		dbMulai = new ais.ui.util.MyDatebox(dateMulai);
		dbMulai.setReadonly(true);
		rangeBox.appendChild(dbMulai);
		rangeBox.appendChild(new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig(" Sampai:")));
		dbSampai = new ais.ui.util.MyDatebox(dateSampai);
		dbSampai.setReadonly(true);
		rangeBox.appendChild(dbSampai);
		row1.appendChild(rangeBox);

		org.zkoss.zul.Hbox satkerBox = new org.zkoss.zul.Hbox();
		satkerBox.setStyle("align-items: center; gap: 8px;");
		satkerBox.appendChild(new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Satuan Kerja:")));
		cbSatker = new AmbilDataSatuanKerjaBanbox();
		if (filterSatker != null) {
			cbSatker.setValue(filterSatker.getNama());
			cbSatker.setAttribute("satuanKerja", filterSatker);
		}
		satkerBox.appendChild(cbSatker);
		row1.appendChild(satkerBox);

		org.zkoss.zul.Hbox cariBox = new org.zkoss.zul.Hbox();
		cariBox.setStyle("align-items: center; gap: 8px; margin-left: 10px;");
		cariBox.appendChild(new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Cari Pegawai:")));
		txtCariPegawai = new org.zkoss.zul.Textbox();
		txtCariPegawai.setWidth("150px");
		if (currentKeyword != null) {
			txtCariPegawai.setValue(currentKeyword);
		}
		cariBox.appendChild(txtCariPegawai);
		row1.appendChild(cariBox);

		org.zkoss.zul.Row row2 = new org.zkoss.zul.Row();
		row2.setStyle("background: transparent; border: none; padding-top: 10px;");
		row2.setParent(filterRows);

		org.zkoss.zul.Vbox hariWrapper = new org.zkoss.zul.Vbox();
		org.zkoss.zul.Label lblHariAktif = new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Hari Aktif Analisis:"));
		lblHariAktif.setStyle("font-weight: 600; color: #495057; margin-bottom: 4px; display: block;");
		hariWrapper.appendChild(lblHariAktif);

		org.zkoss.zul.Hbox checkboxesBox = new org.zkoss.zul.Hbox();
		checkboxesBox.setStyle("gap: 12px; flex-wrap: wrap; align-items: center;");

		chkHaris = new MyCheckboxConfig[Common.haris.length];
		int dayCounter = 1;
		for (String hName : Common.haris) {
			chkHaris[dayCounter - 1] = new MyCheckboxConfig(hName);
			chkHaris[dayCounter - 1].setChecked(activeDays.contains(dayCounter));
			chkHaris[dayCounter - 1].setValue(hName);
			chkHaris[dayCounter - 1].setAttribute("hari", dayCounter);
			checkboxesBox.appendChild(chkHaris[dayCounter - 1]);
			dayCounter++;
		}
		hariWrapper.appendChild(checkboxesBox);
		row2.appendChild(hariWrapper);

		MyToolbarbuttonConfig btnCariDasbor = new MyToolbarbuttonConfig("Tampilkan Analisis", "/img/svg/search.svg");
		btnCariDasbor.setStyle(
				"font-weight: bold; background-color: #0d6efd; color: #ffffff; padding: 6px 16px; border-radius: 6px;");
		btnCariDasbor.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				tabDashboardPanel.setAttribute("keywordCari", txtCariPegawai.getValue());
				reloadHandler.reload(dbMulai.getValue(), dbSampai.getValue(), getSelectedActiveDays(),
						(ais.database.model.rab.SatuanKerja) cbSatker.getAttribute("satuanKerja"), currentSort);
			}
		});
		row2.appendChild(btnCariDasbor);
	}

	private List<Integer> getSelectedActiveDays() {
		List<Integer> nextActiveDays = new java.util.ArrayList<Integer>();
		if (chkHaris != null) {
			for (MyCheckboxConfig chk : chkHaris) {
				if (chk.isChecked()) {
					nextActiveDays.add((Integer) chk.getAttribute("hari"));
				}
			}
		}
		return nextActiveDays;
	}

	private PortalRefs createPortalLayout() {
		PortalRefs refs = new PortalRefs();
		ais.ui.util.MyPortallayout portalLayout = new ais.ui.util.MyPortallayout();
		portalLayout.setWidth("100%");
		portalLayout.setParent(tabDashboardPanel);

		String pcWidth = Common.isMobile() ? "100%" : "50%";

		refs.pcTop = new ais.ui.util.MyPortalchildren();
		refs.pcTop.setWidth("100%");
		refs.pcTop.setParent(portalLayout);

		refs.pcLeft = new ais.ui.util.MyPortalchildren();
		refs.pcLeft.setWidth(pcWidth);
		refs.pcLeft.setStyle("padding: 5px;");
		refs.pcLeft.setParent(portalLayout);

		refs.pcRight = new ais.ui.util.MyPortalchildren();
		refs.pcRight.setWidth(pcWidth);
		refs.pcRight.setStyle("padding: 5px;");
		refs.pcRight.setParent(portalLayout);

		refs.pcBottom = new ais.ui.util.MyPortalchildren();
		refs.pcBottom.setWidth("100%");
		refs.pcBottom.setStyle("padding: 5px; margin-top: 15px;");
		refs.pcBottom.setParent(portalLayout);
		return refs;
	}

	@SuppressWarnings("unchecked")
	private DashboardData loadAndCalculateData() throws Exception {
		return loadAndCalculateData(null);
	}

	@SuppressWarnings("unchecked")
	private DashboardData loadAndCalculateData(ProgressHandler progressHandler) throws Exception {
		DashboardData d = new DashboardData();
		d.progressHandler = progressHandler;
		d.bebanLemburPegawaiMax = getBebanLemburPegawaiMax();
		this.data = d;
		Session session = HibernateUtil.currentSession();

		try {
			progress(progressHandler, "Menyiapkan daftar satuan kerja dan filter akses...", 12);
			java.util.Set<ais.database.model.rab.SatuanKerja> satuanKerjas = new java.util.HashSet<ais.database.model.rab.SatuanKerja>();
			if (filterSatker != null) {
				satuanKerjas.add(filterSatker);
				try {
					new ais.action.master.rab.util.SatuanKerjaTreeModel(false).getChildsSet(filterSatker, satuanKerjas);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:476");
				}
			}

			progress(progressHandler, "Mengecek pegawai login dan hak akses dashboard...", 16);
			Pegawai tbmPeg = getPegawaiLoginNonAdmin();
			List<Pegawai> pegawais = new java.util.ArrayList<Pegawai>();
			if (tbmPeg != null) {
				pegawais.add(tbmPeg);
			} else {
				Criteria critPeg = session.createCriteria(StatuskehadiranKaryawanHarian.class)
						.add(Restrictions.ne("statusabsensi.id", 5L))
						.add(Restrictions.between("tanggal", dateMulai, dateSampai)).createAlias("pegawai", "pegawai")
						.add(Restrictions.eq("pegawai.statusPegawai", ais.common.ConstantValues.AKTIF_PEGAWAI))
						.createAlias("pegawai.tipePegawai", "tipePegawai", Criteria.LEFT_JOIN)
						.add(Restrictions.or(Restrictions.isNull("tipePegawai.masukPresensi"),
								Restrictions.eq("tipePegawai.masukPresensi", true)))
						.add(Restrictions.or(Restrictions.eq("pegawai.aktif", true),
								Restrictions.isNull("pegawai.aktif")));

				if (!satuanKerjas.isEmpty()) {
					critPeg.add(Restrictions.in("pegawai.satuanKerja", satuanKerjas));
				}

				if (currentKeyword != null && !currentKeyword.trim().isEmpty()) {
					String kw = "%" + currentKeyword.trim().toLowerCase() + "%";
					critPeg.add(Restrictions.or(Restrictions.ilike("pegawai.nama", kw), Restrictions
							.or(Restrictions.ilike("pegawai.mycode", kw), Restrictions.ilike("pegawai.code", kw))));
				}

				pegawais = ais.common.ConstantValues.simpleList(
						critPeg.setProjection(Projections.groupProperty("pegawai.id")), Pegawai.class, false);
			}

			if (pegawais == null || pegawais.isEmpty()) {
				progress(progressHandler, "Tidak ada pegawai yang sesuai filter.", 80);
				return d;
			}

			progress(progressHandler, "Ditemukan " + pegawais.size() + " pegawai. Mengambil konfigurasi cuti bersama...", 28);
			Calendar calYear = ais.ui.util.WaktuUtil.getCalendar();
			calYear.setTime(dateMulai);
			Integer selectedtahun = calYear.get(Calendar.YEAR);

			CutiBersama cb = (CutiBersama) session.createCriteria(CutiBersama.class)
					.add(Restrictions.eq("tahun", selectedtahun)).setMaxResults(1).uniqueResult();
			final CutiBersama cutiBersama = (cb != null) ? cb : new CutiBersama();

			progress(progressHandler, "Mengambil data cuti dan izin yang sudah disetujui...", 34);
			final List<ais.database.model.payroll.CutiDanIzin> cutiDanIzinsSemua = session
					.createCriteria(ais.database.model.payroll.CutiDanIzin.class)
					.add(Restrictions.or(Restrictions.between("mulai", dateMulai, dateSampai),
							Restrictions.between("sampai", dateMulai, dateSampai)))
					.addOrder(Order.asc("mulai")).add(Restrictions.in("pegawai", pegawais))
					.add(Restrictions.eq("setujui", true)).list();

			progress(progressHandler, "Membangun status default presensi harian, libur, cuti, dan izin...", 42);
			final java.util.Map<String, StatuskehadiranKaryawanHarian> dbRecords = ais.common.CommonPayroll
					.getDefaultStatuskehadiranKaryawanHarian(cutiDanIzinsSemua, dateMulai, dateSampai, pegawais,
							session, true);

			progress(progressHandler, "Mengambil data pengajuan pegawai yang relevan dengan periode...", 50);
			final List<PengajuanPegawai> pengajuanPegawaisSemua = session
					.createCriteria(
							PengajuanPegawai.class)
					.add(Restrictions.or(
							Restrictions.sqlRestriction(
									"date('" + Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate())
											+ "') between date(this_.waktu) and date(this_.waktusampai)"),
							Restrictions.or(Restrictions.between("waktuSampai", dateMulai, dateSampai),
									Restrictions.between("waktu", dateMulai, dateSampai))))
					.addOrder(Order.asc("waktu")).add(Restrictions.in("pegawai", pegawais))
					.add(Restrictions.eq("setujui", true)).list();

			final java.util.Map<Long, List<PengajuanPegawai>> mapPengajuanPerPegawai = new java.util.HashMap<Long, List<PengajuanPegawai>>();
			for (PengajuanPegawai p : pengajuanPegawaisSemua) {
				Long pId = p.getPegawai().getId();
				List<PengajuanPegawai> listP = mapPengajuanPerPegawai.get(pId);
				if (listP == null) {
					listP = new java.util.ArrayList<PengajuanPegawai>();
					mapPengajuanPerPegawai.put(pId, listP);
				}
				listP.add(p);
			}

			Date sekarang = ais.ui.util.WaktuUtil.getDate();

			progress(progressHandler, "Menghitung metrik harian per pegawai...", 58);
			int nomorPegawai = 0;
			int totalPegawaiProgress = pegawais.size();
			for (Pegawai pegawai : pegawais) {
				nomorPegawai++;
				if (nomorPegawai == 1 || nomorPegawai == totalPegawaiProgress || nomorPegawai % 10 == 0) {
					int persen = 58 + (int) ((nomorPegawai * 20.0) / (totalPegawaiProgress == 0 ? 1 : totalPegawaiProgress));
					progress(progressHandler, "Menghitung presensi pegawai " + nomorPegawai + "/" + totalPegawaiProgress + "...", persen);
				}
				RingkasanPegawaiHolder h = createRingkasanPegawaiHolder(pegawai, cutiBersama);
				java.util.Map<String, List<PengajuanPegawai>> tglsTugas = buildMapPengajuanTanggal(
						mapPengajuanPerPegawai.get(pegawai.getId()));
				h.jumlahPengajuan = mapPengajuanPerPegawai.get(pegawai.getId()) == null ? 0
						: mapPengajuanPerPegawai.get(pegawai.getId()).size();

				Calendar cDaily = Calendar.getInstance();
				cDaily.setTime(dateMulai);
				Calendar sDaily = Calendar.getInstance();
				sDaily.setTime(dateSampai);
				sDaily.add(Calendar.DATE, 1);

				while (cDaily.before(sDaily)) {
					Date tanggal = cDaily.getTime();
					processDailyAttendance(d, h, pegawai, tanggal, sekarang, dbRecords, tglsTugas);
					cDaily.add(Calendar.DATE, 1);
				}
				updateSisaCutiPegawai(h);
				d.mapRingkasan.put(String.valueOf(pegawai.getId()), h);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		progress(progressHandler, "Menerapkan batas maksimal lembur dan menyiapkan agregasi akhir...", 79);
		applyBatasMaksimalLembur(d);
		progress(progressHandler, "Data dashboard berhasil dihitung.", 81);
		return d;
	}

	private void progress(ProgressHandler progressHandler, String pesan, int persen) {
		try {
			if (progressHandler != null) {
				progressHandler.update(pesan, persen);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:606");
		}
	}

	/**
	 * Jika tipe tbmuser diketahui, lebih baik ganti reflection ini dengan akses
	 * langsung seperti kode lama.
	 */
	private Pegawai getPegawaiLoginNonAdmin() {
		try {
			if (tbmuser == null)
				return null;
			Object hakAkses = tbmuser.getClass().getMethod("hakAkses").invoke(tbmuser);
			Object pegawai = tbmuser.getClass().getMethod("getPegawai").invoke(tbmuser);
			if (hakAkses != null && pegawai != null) {
				String roleName = String.valueOf(hakAkses.getClass().getMethod("getRoleName").invoke(hakAkses));
				if (roleName != null && !roleName.toLowerCase().contains("admin")) {
					return (Pegawai) pegawai;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:626");
		}
		return null;
	}

	private RingkasanPegawaiHolder createRingkasanPegawaiHolder(Pegawai pegawai, CutiBersama cutiBersama) {
		RingkasanPegawaiHolder h = new RingkasanPegawaiHolder();
		h.namaPegawai = pegawai.getNama();
		h.namaSatker = pegawai.getSatuanKerja() != null ? pegawai.getSatuanKerja().getNama() : "Tanpa Unit";
		int jatahCuti = pegawai.getJatahCutiTahunan() == null
				? (cutiBersama.getJumlahCuti() != null ? cutiBersama.getJumlahCuti() : 0)
				: pegawai.getJatahCutiTahunan();
		int jatahCutiBersama = cutiBersama.getJumlahCutiBersama() != null ? cutiBersama.getJumlahCutiBersama() : 0;
		h.jumlahCutiTotal = jatahCuti;
		h.jumlahCutiBersama = jatahCutiBersama;
		h.jumlahCutiYangBisaDiambil = jatahCuti - jatahCutiBersama;
		return h;
	}

	private java.util.Map<String, List<PengajuanPegawai>> buildMapPengajuanTanggal(List<PengajuanPegawai> pengajuanP) {
		java.util.Map<String, List<PengajuanPegawai>> tglsTugas = new java.util.HashMap<String, List<PengajuanPegawai>>();
		if (pengajuanP == null)
			pengajuanP = new java.util.ArrayList<PengajuanPegawai>();
		for (PengajuanPegawai pp : pengajuanP) {
			Calendar calendarSub = ais.ui.util.WaktuUtil.getCalendar();
			calendarSub.setTime(pp.getWaktu());
			while (!calendarSub.getTime().after(pp.getWaktuSampai())) {
				Integer hari = calendarSub.get(Calendar.DAY_OF_WEEK);
				if (activeDays.contains(hari)) {
					String keyTanggal = Common.dateFormat85.get().format(calendarSub.getTime());
					List<PengajuanPegawai> listData = tglsTugas.get(keyTanggal);
					if (listData == null) {
						listData = new java.util.ArrayList<PengajuanPegawai>();
						tglsTugas.put(keyTanggal, listData);
					}
					listData.add(pp);
				}
				calendarSub.add(Calendar.DATE, 1);
			}
		}
		return tglsTugas;
	}

	private void processDailyAttendance(DashboardData d, RingkasanPegawaiHolder h, Pegawai pegawai, Date tanggal,
			Date sekarang, java.util.Map<String, StatuskehadiranKaryawanHarian> dbRecords,
			java.util.Map<String, List<PengajuanPegawai>> tglsTugas) {
		boolean holiday = Common.isHoliday(tanggal);
		Calendar cal = Calendar.getInstance();
		cal.setTime(tanggal);
		Integer hari = cal.get(Calendar.DAY_OF_WEEK);

		StatuskehadiranKaryawanHarian skh = dbRecords
				.get(Common.dateFormat83.get().format(tanggal) + "_" + pegawai.getId());
		boolean adaHadir = (skh != null && skh.getStatusabsensi() != null && skh.getStatusabsensi().getId().equals(1L));

		if (skh == null) {
			skh = new StatuskehadiranKaryawanHarian();
			skh.setTanggal(tanggal);
			skh.setPegawai(pegawai);
			skh.setStatusabsensi(tanggal.before(sekarang) ? ais.common.ConstantValues.TIDAK_ADA_ALASAN
					: ais.common.ConstantValues.BELUM_ABSEN);
		}

		boolean isNationalHoliday = skh.getLiburNasional() != null;
		boolean hariAktifDipilih = activeDays != null && activeDays.contains(hari);
		boolean hariLiburDitentukanOlehShift = isHariLiburDitentukanOlehShift(skh);
		holiday = hitungHariLiburPresensi(skh, tanggal, hari, holiday);
		if (holiday) {
			d.totalHariLiburGlobal++;
			d.hariLiburLogKeys.add(buildLogHarianKey(skh));
		}
		ais.database.model.payroll.CutiDanIzin cutiDanIzin = skh.getCutiDanIzin();

		accumulateCutiIzinDisetujui(d, h, skh, cutiDanIzin);

		if (!holiday && (hariAktifDipilih || hariLiburDitentukanOlehShift))
			h.aktif++;
		if (skh.isTidakHadirEfektif(adaHadir, holiday, skh.getCutiDanIzin(), skh.getLiburNasional()))
			h.tidak_hadir++;
		if (!holiday && skh.isTidakHadirEfektif(adaHadir, holiday, skh.getCutiDanIzin(), skh.getLiburNasional()))
			h.tidakHadirTanpaHoliday++;

		putStatusKondisi(d, getKondisiKehadiran(skh, cutiDanIzin, holiday, isNationalHoliday));
		accumulateMetrikPegawai(h, skh, pegawai, cutiDanIzin, holiday);
		addInfoPengajuanToKeterangan(skh, tanggal, tglsTugas);

		Double lemburDa = getLemburValid(skh, pegawai);
		if (lemburDa > 0.01) {
			d.totalLemburGlobal++;
			d.totalJamLemburGlobal += lemburDa;
		}

		accumulateGlobalAndSatker(d, h, skh, holiday, lemburDa);
		accumulateTrendHarian(d, tanggal, skh, lemburDa);

		d.listKehadiranFiltered.add(skh);
	}

	private KondisiInfo getKondisiKehadiran(StatuskehadiranKaryawanHarian skh,
			ais.database.model.payroll.CutiDanIzin cutiDanIzin, boolean holiday, boolean isNationalHoliday) {
		KondisiInfo ki = new KondisiInfo();
		ki.textColor = "#ffffff";
		if (cutiDanIzin != null && Boolean.TRUE.equals(cutiDanIzin.getSetujui())) {
			ki.status = cutiDanIzin.getStatusabsensi() != null ? cutiDanIzin.getStatusabsensi().getNama()
					: "Cuti / Izin";
			ki.bgColor = "#eb3434";
		} else if (holiday && skh.ambilMasukjam() != null) {
			ki.status = "Masuk Waktu Libur";
			ki.bgColor = "#1251ff";
		} else if (holiday && skh.ambilMasukjam() == null) {
			ki.status = "Libur";
			ki.bgColor = "#803443";
		} else if (Boolean.TRUE.equals(skh.getDatangCepat()) && Boolean.TRUE.equals(skh.getPulangTerlambat())) {
			ki.status = "Hadir Cepat Pulang Terlambat";
			ki.bgColor = "#d602d6";
		} else if (Boolean.TRUE.equals(skh.getDatangTerlambat())
				&& (cutiDanIzin == null || !Boolean.TRUE.equals(cutiDanIzin.getSetujui()))) {
			ki.status = "Hadir Terlambat";
			ki.bgColor = "#ffbf00";
			ki.textColor = "#000000";
		} else if (Boolean.TRUE.equals(skh.getPulangCepat())
				&& (cutiDanIzin == null || !Boolean.TRUE.equals(cutiDanIzin.getSetujui()))) {
			ki.status = "Pulang Cepat";
			ki.bgColor = "#a1fffd";
			ki.textColor = "#000000";
		} else if (Boolean.TRUE.equals(skh.getPulangTerlambat())) {
			ki.status = "Pulang Terlambat";
			ki.bgColor = "#08b502";
		} else if (Boolean.TRUE.equals(skh.getDatangCepat())) {
			ki.status = "Hadir Cepat";
			ki.bgColor = "#ebe534";
			ki.textColor = "#000000";
		} else if (skh.ambilMasukjam() == null
				&& (cutiDanIzin == null || !Boolean.TRUE.equals(cutiDanIzin.getSetujui()))) {
			ki.status = "Tidak Hadir (Alpa)";
			ki.bgColor = "#eb3434";
		} else if (skh.ambilPulangjam() == null) {
			ki.status = "Tidak Absen Pulang";
			ki.bgColor = "#c2d4f0";
			ki.textColor = "#000000";
		} else {
			ki.status = "Sesuai / Tepat Waktu";
			ki.bgColor = "#f2f0f0";
			ki.textColor = "#000000";
		}
		return ki;
	}

	private void putStatusKondisi(DashboardData d, KondisiInfo ki) {
		Integer currentCount = d.statusKondisiCount.get(ki.status);
		d.statusKondisiCount.put(ki.status, currentCount == null ? 1 : currentCount + 1);
		d.statusKondisiBg.put(ki.status, ki.bgColor);
		d.statusKondisiColor.put(ki.status, ki.textColor);
	}

	private void accumulateMetrikPegawai(RingkasanPegawaiHolder h, StatuskehadiranKaryawanHarian skh, Pegawai pegawai,
			ais.database.model.payroll.CutiDanIzin cutiDanIzin, boolean holiday) {
		h.jamMasuk += skh.getJumlahJamMasuk() != null ? skh.getJumlahJamMasuk() : 0.0;
		h.terlambatJam += skh.getJumlahTerlambat() != null ? skh.getJumlahTerlambat() : 0.0;
		h.lemburMasuk += getLemburValid(skh, pegawai);
		h.cepatKeluar += skh.getJumlahCepatKeluar() != null ? skh.getJumlahCepatKeluar() : 0.0;
		h.cepatJam += skh.getJumlahCepat() != null ? skh.getJumlahCepat() : 0.0;
		h.sebelumWaktu += skh.getJumlahMasukSebelumWaktunya() != null ? skh.getJumlahMasukSebelumWaktunya() : 0.0;
		h.setelahWaktu += skh.getJumlahPulangSetelahWaktunya() != null ? skh.getJumlahPulangSetelahWaktunya() : 0.0;

		// Cuti memotong dihitung khusus di accumulateCutiIzinDisetujui(...)
		// memakai cutiData.getJumlahHariCuti() dan dikunci per ID pengajuan agar tidak dobel
		// saat loop harian melewati tanggal cuti yang sama.
		if (skh.ambilMasukjam() != null && skh.ambilPulangjam() == null)
			h.tidakAbsenPulang++;
		if (!holiday)
			h.jumlahHariEfektif++;
	}

	private boolean hitungHariLiburPresensi(StatuskehadiranKaryawanHarian skh, Date tanggal, Integer hari,
			boolean defaultHoliday) {
		boolean holiday = defaultHoliday;
		try {
			if (skh != null && skh.getLiburNasional() != null) {
				holiday = true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:807");
		}

		if (hari != null && (activeDays == null || !activeDays.contains(hari))) {
			holiday = true;
		}

		if (isHariLiburDitentukanOlehShift(skh)) {
			return isKhususBuatHariLibur(skh);
		}
		return holiday;
	}

	private boolean isHariLiburDitentukanOlehShift(StatuskehadiranKaryawanHarian skh) {
		try {
			return skh != null
					&& skh.getDetailJenisShiftPegawai() != null
					&& skh.getDetailJenisShiftPegawai().getJenisShiftPegawai() != null
					&& Boolean.TRUE.equals(skh.getDetailJenisShiftPegawai().getJenisShiftPegawai()
							.getHariLiburDitentukan());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:827");
		}
		return false;
	}

	private boolean isKhususBuatHariLibur(StatuskehadiranKaryawanHarian skh) {
		try {
			return skh != null && skh.getDetailJenisShiftPegawai() != null
					&& Boolean.TRUE.equals(skh.getDetailJenisShiftPegawai().getKhususBuatHariLibur());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:836");
		}
		return false;
	}

	private String buildLogHarianKey(StatuskehadiranKaryawanHarian skh) {
		String pegawaiKey = skh != null && skh.getPegawai() != null && skh.getPegawai().getId() != null
				? String.valueOf(skh.getPegawai().getId())
				: "pegawai-null";
		String tanggalKey = skh != null && skh.getTanggal() != null ? Common.dateFormat83.get().format(skh.getTanggal())
				: "tanggal-null";
		return tanggalKey + "_" + pegawaiKey;
	}

	private boolean isLogHariLibur(StatuskehadiranKaryawanHarian skh) {
		return data != null && data.hariLiburLogKeys != null && data.hariLiburLogKeys.contains(buildLogHarianKey(skh));
	}

	private void accumulateCutiIzinDisetujui(DashboardData d, RingkasanPegawaiHolder h,
			StatuskehadiranKaryawanHarian skh, ais.database.model.payroll.CutiDanIzin cutiData) {
		if (cutiData == null || !Boolean.TRUE.equals(cutiData.getSetujui())) {
			return;
		}

		String keyCuti = buildCutiIzinKey(skh, cutiData);
		int jumlahHariCuti = getJumlahHariCutiSafe(cutiData);

		if (!d.cutiIzinGlobalKeys.contains(keyCuti)) {
			d.cutiIzinGlobalKeys.add(keyCuti);
			d.totalCutiIzinGlobal += jumlahHariCuti;
		}

		if (Boolean.TRUE.equals(cutiData.getMemotongJatahCuti())) {
			if (!d.cutiMemotongGlobalKeys.contains(keyCuti)) {
				d.cutiMemotongGlobalKeys.add(keyCuti);
				d.totalCutiMemotongGlobal += jumlahHariCuti;
			}
			if (!h.cutiIzinKeys.contains(keyCuti)) {
				h.cutiIzinKeys.add(keyCuti);
				h.cuti_memotong += jumlahHariCuti;
			}
		} else {
			if (!d.cutiTidakMemotongGlobalKeys.contains(keyCuti)) {
				d.cutiTidakMemotongGlobalKeys.add(keyCuti);
				d.totalCutiTidakMemotongGlobal += jumlahHariCuti;
			}
			if (!h.cutiIzinKeys.contains(keyCuti)) {
				h.cutiIzinKeys.add(keyCuti);
				h.cuti_tidak_memotong += jumlahHariCuti;
			}
		}
	}

	private String buildCutiIzinKey(StatuskehadiranKaryawanHarian skh, ais.database.model.payroll.CutiDanIzin cutiData) {
		String pegawaiKey = skh != null && skh.getPegawai() != null && skh.getPegawai().getId() != null
				? String.valueOf(skh.getPegawai().getId())
				: "pegawai-null";

		try {
			Object id = cutiData.getClass().getMethod("getId").invoke(cutiData);
			if (id != null) {
				return pegawaiKey + "_CUTI_" + String.valueOf(id);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:899");
		}

		String mulai = cutiData.getMulai() != null ? Common.dateFormat83.get().format(cutiData.getMulai()) : "mulai-null";
		String sampai = cutiData.getSampai() != null ? Common.dateFormat83.get().format(cutiData.getSampai()) : "sampai-null";
		String status = cutiData.getStatusabsensi() != null && cutiData.getStatusabsensi().getId() != null
				? String.valueOf(cutiData.getStatusabsensi().getId())
				: "status-null";
		return pegawaiKey + "_CUTI_" + mulai + "_" + sampai + "_" + status;
	}

	private int getJumlahHariCutiSafe(ais.database.model.payroll.CutiDanIzin cutiData) {
		int jumlahHariCuti = 1;
		try {
			jumlahHariCuti = cutiData.getJumlahHariCuti();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:914");
		}
		return jumlahHariCuti <= 0 ? 1 : jumlahHariCuti;
	}

	private void updateSisaCutiPegawai(RingkasanPegawaiHolder h) {
		int sisa = h.jumlahCutiTotal - h.jumlahCutiBersama - (int) h.cuti_memotong;
		h.jumlahCutiYangBisaDiambil = sisa < 0 ? 0 : sisa;
	}

	private void addInfoPengajuanToKeterangan(StatuskehadiranKaryawanHarian skh, Date tanggal,
			java.util.Map<String, List<PengajuanPegawai>> tglsTugas) {
		String keyTanggalP = Common.dateFormat85.get().format(tanggal);
		List<PengajuanPegawai> pengajuanPegawaisData = tglsTugas.get(keyTanggalP);
		if (pengajuanPegawaisData == null || pengajuanPegawaisData.isEmpty())
			return;

		String infoPengajuan = "";
		for (PengajuanPegawai pjp : pengajuanPegawaisData) {
			infoPengajuan += " [" + pjp.getJenisPengajuanPegawai().getNama() + "]";
		}
		if (!infoPengajuan.isEmpty()) {
			skh.setKeterangan((skh.getKeterangan() != null ? skh.getKeterangan() : "") + infoPengajuan);
		}
	}

	private Double getLemburValid(StatuskehadiranKaryawanHarian skh, Pegawai pegawai) {
		try {
			if (pegawai != null && pegawai.getTipePegawai() != null
					&& Boolean.TRUE.equals(pegawai.getTipePegawai().getMasukLembur())) {
				double jamLembur = skh.getJumlahLemburMasuk() != null ? skh.getJumlahLemburMasuk() : 0.0;
				return batasiJamLembur(jamLembur, data != null ? data.bebanLemburPegawaiMax : getBebanLemburPegawaiMax());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:947");
		}
		return 0.0;
	}

	private int getBebanLemburPegawaiMax() {
		int bebanLemburPegawaiMax = -1;
		try {
			bebanLemburPegawaiMax = Integer.parseInt(Common
					.getKonfigurasi("beban_lembur_pegawai_max", bebanLemburPegawaiMax + "")
					.getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:958");
		}
		return bebanLemburPegawaiMax;
	}

	private double batasiJamLembur(double jamLembur, int bebanLemburPegawaiMax) {
		if (jamLembur < 0.0) {
			return 0.0;
		}
		if (bebanLemburPegawaiMax != -1 && jamLembur > bebanLemburPegawaiMax) {
			return bebanLemburPegawaiMax;
		}
		return jamLembur;
	}

	private void applyBatasMaksimalLembur(DashboardData d) {
		if (d == null || d.bebanLemburPegawaiMax == -1) {
			return;
		}

		d.totalLemburGlobal = 0;
		d.totalJamLemburGlobal = 0.0;
		for (RingkasanSatkerHolder sh : d.mapSatker.values()) {
			sh.totalJamLembur = 0.0;
		}

		for (RingkasanPegawaiHolder h : d.mapRingkasan.values()) {
			h.lemburMasuk = batasiJamLembur(h.lemburMasuk, d.bebanLemburPegawaiMax);
			if (h.lemburMasuk > 0.01) {
				d.totalLemburGlobal++;
				d.totalJamLemburGlobal += h.lemburMasuk;
			}
			RingkasanSatkerHolder sh = d.mapSatker.get(h.namaSatker);
			if (sh != null) {
				sh.totalJamLembur += h.lemburMasuk;
			}
		}

		for (double[] v : d.dataTrenHarian.values()) {
			if (v != null && v.length > 1) {
				v[1] = batasiJamLembur(v[1], d.bebanLemburPegawaiMax);
			}
		}
	}

	private void accumulateGlobalAndSatker(DashboardData d, RingkasanPegawaiHolder h, StatuskehadiranKaryawanHarian skh,
			boolean holiday, Double lemburDa) {
		if (skh.ambilMasukjam() != null && skh.ambilPulangjam() == null)
			d.totalTidakAbsenPulangGlobal++;
		if (holiday && skh.ambilMasukjam() != null)
			d.totalMasukHariLiburGlobal++;

		Statusabsensi statusabsensi = skh.getStatusabsensi();
		boolean cutiApproved = skh.getCutiDanIzin() != null && Boolean.TRUE.equals(skh.getCutiDanIzin().getSetujui());
		boolean adaHadir = statusabsensi != null && statusabsensi.getId().equals(1L);
		boolean adaPresensiAktual = skh.ambilMasukjam() != null || skh.ambilPulangjam() != null || adaHadir;

		if (holiday && !adaPresensiAktual && !cutiApproved) {
			return;
		}

		if (adaHadir || skh.getCutiDanIzin() == null || !cutiApproved) {
			if (Boolean.TRUE.equals(skh.getDatangTerlambat()))
				h.terlambat++;
			else if (Boolean.TRUE.equals(skh.getDatangCepat()))
				h.pulangcepat++;
			else
				h.tepatWaktu++;

			if (skh.getMasukjam() != null && skh.getDetailJenisShiftPegawai() != null
					&& skh.getDetailJenisShiftPegawai().getMulai() != null) {
				try {
					if (Double.parseDouble(
							Common.timeFormat2.get().format(skh.getDetailJenisShiftPegawai().getMulai())) >= Double
									.parseDouble(Common.timeFormat2.get().format(skh.getMasukjam()))) {
						h.tepatWaktuBanget++;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranExpert.java:1035");
				}
			}

			if (statusabsensi != null) {
				if (statusabsensi.getId().equals(1L))
					h.masuk++;
				else if (!holiday && statusabsensi.getId().equals(2L))
					h.alpa++;
				else if (statusabsensi.getId().equals(3L))
					h.sakit++;
				else if (statusabsensi.getId().equals(4L))
					h.izin++;
				else if (statusabsensi.getId().equals(5L))
					h.belum++;
				else
					h.lain++;
			}

			d.totalLogGlobal++;
			if (statusabsensi != null) {
				if (statusabsensi.getId().equals(1L)) {
					if (Boolean.TRUE.equals(skh.getDatangTerlambat()))
						d.totalHadirTerlambatGlobal++;
					else
						d.totalHadirTepatWaktuGlobal++;
					if (Boolean.TRUE.equals(skh.getPulangCepat()))
						d.totalPulangCepatGlobal++;
				} else if (statusabsensi.getId().equals(2L)) {
					if (!holiday)
						d.totalAlphaGlobal++;
				} else if (statusabsensi.getId().equals(3L) || statusabsensi.getId().equals(4L)) {
					d.totalCutiIzinGlobal++;
				}
			}
		}

		String satkerKey = h.namaSatker;
		RingkasanSatkerHolder sh = d.mapSatker.get(satkerKey);
		if (sh == null) {
			sh = new RingkasanSatkerHolder();
			sh.namaSatker = satkerKey;
			d.mapSatker.put(satkerKey, sh);
		}
		sh.totalLog++;
		if (statusabsensi != null && statusabsensi.getId().equals(1L))
			sh.totalHadir++;
		if (statusabsensi != null && statusabsensi.getId().equals(1L) && Boolean.TRUE.equals(skh.getDatangTerlambat()))
			sh.totalTelat++;
		if (statusabsensi != null && statusabsensi.getId().equals(2L) && !holiday)
			sh.totalAlpha++;
		sh.totalJamLembur += lemburDa;
	}

	private void accumulateTrendHarian(DashboardData d, Date tanggal, StatuskehadiranKaryawanHarian skh,
			Double lemburDa) {
		String tglKey = Common.dateFormat.get().format(tanggal);
		if (!d.dataTrenHarian.containsKey(tglKey))
			d.dataTrenHarian.put(tglKey, new double[7]);
		double[] v = d.dataTrenHarian.get(tglKey);
		v[0] += skh.getJumlahJamMasuk() != null ? skh.getJumlahJamMasuk() : 0.0;
		v[1] += lemburDa;
		v[2] += skh.getJumlahTerlambat() != null ? skh.getJumlahTerlambat() : 0.0;
		v[3] += skh.getJumlahCepatKeluar() != null ? skh.getJumlahCepatKeluar() : 0.0;
		v[4] += skh.getJumlahCepat() != null ? skh.getJumlahCepat() : 0.0;
		v[5] += skh.getJumlahMasukSebelumWaktunya() != null ? skh.getJumlahMasukSebelumWaktunya() : 0.0;
		v[6] += skh.getJumlahPulangSetelahWaktunya() != null ? skh.getJumlahPulangSetelahWaktunya() : 0.0;
	}

	private void sortDailyLogs() {
		java.util.Collections.sort(data.listKehadiranFiltered,
				new java.util.Comparator<StatuskehadiranKaryawanHarian>() {
					public int compare(StatuskehadiranKaryawanHarian o1, StatuskehadiranKaryawanHarian o2) {
						if ("waktu_asc".equals(currentSort))
							return o1.getTanggal().compareTo(o2.getTanggal());
						else if ("nama_asc".equals(currentSort))
							return safeNama(o1).compareToIgnoreCase(safeNama(o2));
						else if ("nama_desc".equals(currentSort))
							return safeNama(o2).compareToIgnoreCase(safeNama(o1));
						else if ("masuk_asc".equals(currentSort))
							return compareDateNullable(o1.ambilMasukjam(), o2.ambilMasukjam(), true);
						else if ("masuk_desc".equals(currentSort))
							return compareDateNullable(o1.ambilMasukjam(), o2.ambilMasukjam(), false);
						else if ("pulang_asc".equals(currentSort))
							return compareDateNullable(o1.ambilPulangjam(), o2.ambilPulangjam(), true);
						else if ("pulang_desc".equals(currentSort))
							return compareDateNullable(o1.ambilPulangjam(), o2.ambilPulangjam(), false);
						else if ("lembur_desc".equals(currentSort))
							return Double.compare(getLemburValid(o2, o2.getPegawai()),
									getLemburValid(o1, o1.getPegawai()));
						else if ("lembur_asc".equals(currentSort))
							return Double.compare(getLemburValid(o1, o1.getPegawai()),
									getLemburValid(o2, o2.getPegawai()));
						return o2.getTanggal().compareTo(o1.getTanggal());
					}
				});
	}

	private String safeNama(StatuskehadiranKaryawanHarian skh) {
		return skh != null && skh.getPegawai() != null && skh.getPegawai().getNama() != null
				? skh.getPegawai().getNama()
				: "";
	}

	private int compareDateNullable(Date d1, Date d2, boolean asc) {
		if (d1 == null && d2 == null)
			return 0;
		if (d1 == null)
			return 1;
		if (d2 == null)
			return -1;
		return asc ? d1.compareTo(d2) : d2.compareTo(d1);
	}

	private org.zkoss.zul.Panelchildren createPanel(String title, Component parent, String style) {
		org.zkoss.zul.Panel pnl = new ais.ui.util.MyPanelConfig();
		pnl.setTitle(title);
		pnl.setBorder("none");
		pnl.setCollapsible(false);
		pnl.setClosable(false);
		pnl.setMaximizable(false);
		pnl.setMinimizable(false);
		pnl.setStyle("margin-bottom:14px; border:1px solid #e5e7eb; border-radius:18px; overflow:hidden;"
				+ "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.07);");
		pnl.setParent(parent);
		org.zkoss.zul.Panelchildren pch = new org.zkoss.zul.Panelchildren();
		pch.setStyle(style == null ? "padding:14px; background:#fff;" : style);
		pch.setParent(pnl);
		appendDashboardDescription(pch, title);
		return pch;
	}

	private void appendDashboardDescription(Component parent, String title) {
		String desc = getDashboardDescription(title);
		if (desc == null || desc.trim().length() == 0) {
			return;
		}
		org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style=\"margin:0 0 12px 0; padding:10px 12px; "
				+ "border-radius:14px; background:#f8fafc; border:1px solid #e2e8f0; color:#475569; "
				+ "font-size:11.5px; line-height:1.6;\"><b style=\"color:#0f172a;\"></b> "
				+ escapeHtml(desc) + "</div>");
		html.setParent(parent);
	}

	private String getDashboardDescription(String title) {
		if (title == null) {
			return "Menyajikan ringkasan kehadiran dalam bahasa yang mudah dibaca agar HRD, pimpinan, dan petugas unit dapat memahami kondisi utama tanpa membuka data mentah satu per satu.";
		}
		String t = title.toLowerCase(java.util.Locale.ENGLISH);
		if (t.indexOf("overview") >= 0 || t.indexOf("presensi") >= 0) {
			return "Menyatukan jumlah log presensi, hari aktif, hadir tepat waktu, terlambat, pulang cepat, cuti, izin, alpha, lembur, dan masalah absen pulang. Tampilan ini menjadi pintu masuk untuk mengetahui apakah kondisi kehadiran periode terpilih sedang normal atau membutuhkan perhatian.";
		}
		if (t.indexOf("perhatian") >= 0 || t.indexOf("watchlist") >= 0 || t.indexOf("risiko") >= 0) {
			return "Mengarahkan perhatian ke pegawai atau kondisi yang perlu segera ditindaklanjuti, seperti sering alpha, terlambat, pulang cepat, tidak absen pulang, atau mulai menunjukkan penurunan kedisiplinan. Daftar ini membantu HRD menentukan prioritas pembinaan tanpa mencari manual di seluruh data.";
		}
		if (t.indexOf("ringkasan riwayat") >= 0) {
			return "Merangkum riwayat kehadiran setiap pegawai dalam satu tabel perbandingan, termasuk hadir, terlambat, cuti, izin, alpha, lembur, dan jam kerja. Data ini membantu pimpinan melihat siapa yang stabil, siapa yang membutuhkan pembinaan, dan unit mana yang perlu dipantau.";
		}
		if (t.indexOf("rincian log") >= 0) {
			return "Menampilkan catatan harian presensi secara detail agar pengguna dapat menelusuri tanggal, status kehadiran, jam masuk, jam pulang, keterlambatan, lembur, dan keterangan setiap pegawai. Tampilan ini berguna untuk verifikasi sebelum mengambil keputusan atau melakukan koreksi data.";
		}
		if (t.indexOf("satuan kerja") >= 0 || t.indexOf("unit") >= 0) {
			return "Membandingkan kedisiplinan antar satuan kerja supaya pimpinan dapat melihat unit yang sudah berjalan baik dan unit yang memerlukan pendampingan. Informasi ini membantu evaluasi kinerja unit tanpa harus mengecek pegawai satu per satu.";
		}
		if (t.indexOf("lembur") >= 0 || t.indexOf("beban kerja") >= 0) {
			return "Membaca beban kerja dari total jam kerja, lembur, dan pola kehadiran sehingga pegawai yang berpotensi kelebihan beban dapat terlihat lebih awal. Data ini berguna untuk mengatur pembagian tugas, validasi lembur, dan menjaga keseimbangan beban kerja.";
		}
		if (t.indexOf("ketidakhadiran") >= 0 || t.indexOf("kondisi") >= 0) {
			return "Memperlihatkan penyebab tidak hadir dan kondisi presensi, seperti sakit, izin, cuti, alpha, terlambat, dan pulang cepat. Tampilan ini membantu pengguna memahami alasan utama ketidakhadiran serta menentukan apakah perlu klarifikasi, pembinaan, atau perbaikan kebijakan.";
		}
		if (t.indexOf("trend") >= 0 || t.indexOf("grafik") >= 0 || t.indexOf("heatmap") >= 0) {
			return "Menampilkan pola perubahan dari hari ke hari atau dari satu kelompok ke kelompok lain agar lonjakan keterlambatan, lembur, alpha, atau beban kerja mudah terlihat. Grafik HTML/CSS ini membantu membaca tren tanpa menghitung angka secara manual.";
		}
		if (t.indexOf("peringkat") >= 0 || t.indexOf("top") >= 0 || t.indexOf("ranking") >= 0) {
			return "Mengurutkan pegawai berdasarkan indikator tertentu agar capaian terbaik dan masalah paling menonjol mudah dikenali. Tampilan ini dapat dipakai untuk apresiasi, evaluasi, atau penentuan prioritas tindak lanjut.";
		}
		if (t.indexOf("produktivitas") >= 0) {
			return "Menggambarkan rata-rata jam kerja, jam lembur, jumlah pegawai yang dianalisis, dan sebaran satuan kerja dalam periode pilihan. Informasi ini membantu membaca produktivitas secara wajar, bukan hanya dari jumlah hadir saja.";
		}
		if (t.indexOf("radar") >= 0 || t.indexOf("kesehatan") >= 0) {
			return "Merangkum beberapa indikator penting kehadiran dalam satu tampilan ringkas, seperti rasio hadir, ketepatan waktu, alpha, lembur, dan kelengkapan absen. Semakin seimbang nilainya, semakin sehat kondisi pengelolaan kehadiran.";
		}
		if (t.indexOf("tindak lanjut") >= 0) {
			return "Menerjemahkan angka-angka kehadiran menjadi arahan kerja yang mudah dipahami, misalnya siapa yang perlu dikonfirmasi, unit mana yang perlu dievaluasi, dan pola apa yang perlu dicegah berulang.";
		}
		return "Menyajikan data kehadiran dalam bentuk ringkas, terarah, dan mudah dibaca agar pengguna dapat memahami kondisi utama serta menentukan tindak lanjut yang tepat.";
	}

	private void addColumns(MyGrid grid, String... titles) {
		org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
		cols.setParent(grid);
		for (String title : titles)
			new ais.ui.util.MyColumnConfig(title).setParent(cols);
	}

	private MyGrid createGrid(Component parent, int pageSize, String width) {
		MyGrid grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(pageSize);
		grid.setSclass("dgrid fgrid table-striped");
		grid.setWidth(width == null ? "100%" : width);
		grid.setParent(parent);
		return grid;
	}

	private void createCard(String title, String value, String sub, String bg, String text, org.zkoss.zul.Hbox p) {
		org.zkoss.zul.Div c = new org.zkoss.zul.Div();
		c.setStyle("padding:10px 8px; background-color:" + bg
				+ "; border-radius:9px; flex:0 1 calc(10% - 8px); max-width:calc(10% - 8px); min-width:96px; box-sizing:border-box; text-align:center; box-shadow:0 2px 4px rgba(0,0,0,0.05);");
		c.setParent(p);
		org.zkoss.zul.Label tLabel = new org.zkoss.zul.Label(title);
		tLabel.setStyle("font-size:10px; font-weight:600; color:" + text + "; opacity:0.8;");
		c.appendChild(tLabel);
		c.appendChild(new org.zkoss.zul.Html("<br/>"));
		org.zkoss.zul.Label vLabel = new org.zkoss.zul.Label(value);
		vLabel.setStyle("font-size:22px; font-weight:700; color:" + text + ";");
		c.appendChild(vLabel);
		c.appendChild(new org.zkoss.zul.Html("<br/>"));
		org.zkoss.zul.Label sLabel = new org.zkoss.zul.Label(sub);
		sLabel.setStyle("font-size:9px; color:" + text + "; opacity:0.7;");
		c.appendChild(sLabel);
	}

	private void renderOverviewPresensi() {
		org.zkoss.zul.Panelchildren pch = createPanel(
				"Overview Presensi (" + Common.dateFormat.get().format(dateMulai) + " s/d "
						+ Common.dateFormat.get().format(dateSampai) + ")",
				portal.pcTop, "padding: 15px; background: #fff;");
		org.zkoss.zul.Hbox cardsBox = new org.zkoss.zul.Hbox();
		cardsBox.setWidth("100%");
		cardsBox.setStyle("display:flex; flex-wrap:wrap; gap:10px; justify-content:center; align-items:stretch;");
		cardsBox.setParent(pch);

		long totalHariAktif = 0L;
		long totalHariEfektif = 0L;
		for (RingkasanPegawaiHolder h : data.mapRingkasan.values()) {
			totalHariAktif += h.aktif;
			totalHariEfektif += h.jumlahHariEfektif;
		}

		createCard("TOTAL LOG", String.valueOf(data.totalLogGlobal), "Baris status presensi", "#f8f9fa", "#212529", cardsBox);
		createCard("HARI AKTIF", String.valueOf(totalHariAktif), "Hari filter aktif", "#e7f1ff", "#084298", cardsBox);
		createCard("HARI EFEKTIF", String.valueOf(totalHariEfektif), "Bukan hari libur", "#f8f9fa", "#212529", cardsBox);
		createCard("TOTAL LIBUR", String.valueOf(data.totalHariLiburGlobal), "Libur/filter/shift", "#e9ecef", "#495057", cardsBox);
		createCard("TEPAT WAKTU", String.valueOf(data.totalHadirTepatWaktuGlobal), "Kehadiran Disiplin", "#d1e7dd",
				"#0f5132", cardsBox);
		createCard("TERLAMBAT", String.valueOf(data.totalHadirTerlambatGlobal), "Perlu Evaluasi", "#fff3cd", "#664d03",
				cardsBox);
		createCard("ALPHA", String.valueOf(data.totalAlphaGlobal), "Mangkir", "#f8d7da", "#842029", cardsBox);
		createCard("CUTI / IZIN", String.valueOf(data.totalCutiIzinGlobal), "Disetujui", "#e0cffc", "#3b0918", cardsBox);
		createCard("CUTI MEMOTONG", String.valueOf(data.totalCutiMemotongGlobal), "Potong jatah cuti", "#f8d7da", "#842029", cardsBox);
		createCard("CUTI TDK MEMOTONG", String.valueOf(data.totalCutiTidakMemotongGlobal), "Tidak potong cuti", "#fff3cd", "#664d03", cardsBox);
		createCard("TIDAK ABSEN PULANG", String.valueOf(data.totalTidakAbsenPulangGlobal), "Perlu Verifikasi",
				"#cff4fc", "#055160", cardsBox);
		createCard("MASUK HARI LIBUR", String.valueOf(data.totalMasukHariLiburGlobal), "Extra Effort", "#cfe2ff",
				"#084298", cardsBox);
		createCard("LEMBUR VALID", String.valueOf(data.totalLemburGlobal),
				Common.numberFormat.get().format(data.totalJamLemburGlobal) + " Jam", "#e2e3e5", "#383d41", cardsBox);
		createCard("PULANG CEPAT", String.valueOf(data.totalPulangCepatGlobal), "Perlu Evaluasi", "#cff4fc", "#055160",
				cardsBox);

		renderDefinisiMetrikOverview(pch);
	}

	private void renderDefinisiMetrikOverview(Component parent) {
		org.zkoss.zul.Html info = new org.zkoss.zul.Html(
				"<div style=\"margin-top:12px; padding:10px 12px; border-radius:8px; background:#f8fafc; border:1px solid #dee2e6; color:#495057; font-size:11px; line-height:1.5;\">"
						+ "<b>Catatan metrik:</b> "
						+ "<b>Total Log</b> adalah jumlah baris status presensi/ketidakhadiran yang ikut masuk rekap overview. "
						+ "<b>Hari Aktif</b> adalah hari analisis sesuai checklist hari aktif, kecuali shift menentukan hari kerja/libur sendiri. "
						+ "<b>Hari Efektif</b> adalah hari yang bukan libur menurut kalender, filter hari aktif, libur nasional, atau aturan shift jaga. <b>Total Libur</b> menghitung hari libur/filter nonaktif/shift libur per pegawai pada periode. "
						+ "<b>Cuti / Izin</b>, <b>Cuti Memotong</b>, dan <b>Cuti Tidak Memotong</b> memakai nilai <code>cutiData.getJumlahHariCuti()</code> dari data pengajuan cuti/izin yang disetujui."
						+ "</div>");
		info.setParent(parent);
	}

	private void renderWatchlistPerhatianKhusus() {
		org.zkoss.zul.Panelchildren pch = createPanel("Daftar Perhatian Khusus (Pegawai Sering Alpha / Terlambat)",
				portal.pcTop, null);
		List<RingkasanPegawaiHolder> watchList = new java.util.ArrayList<RingkasanPegawaiHolder>(
				data.mapRingkasan.values());
		java.util.Collections.sort(watchList, new java.util.Comparator<RingkasanPegawaiHolder>() {
			public int compare(RingkasanPegawaiHolder a, RingkasanPegawaiHolder b) {
				long scoreA = (a.alpa * 3) + a.terlambat + a.tidakAbsenPulang;
				long scoreB = (b.alpa * 3) + b.terlambat + b.tidakAbsenPulang;
				return Long.compare(scoreB, scoreA);
			}
		});

		MyGrid grid = createGrid(pch, 10, "100%");
		addColumns(grid, "Prioritas", "Nama Pegawai", "Satuan Kerja", "Frekuensi Alpha", "Frekuensi Terlambat",
				"Tidak Absen Pulang", "Saran Tindakan");
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		rows.setParent(grid);
		int urutan = 1;
		for (RingkasanPegawaiHolder w : watchList) {
			if ((w.alpa + w.terlambat + w.tidakAbsenPulang) <= 0)
				continue;
			org.zkoss.zul.Row r = new org.zkoss.zul.Row();
			r.setParent(rows);
			org.zkoss.zul.Label lblUrut = new org.zkoss.zul.Label("Peringatan #" + urutan);
			lblUrut.setStyle("color:#dc3545; font-weight:bold;");
			r.appendChild(lblUrut);
			r.appendChild(new org.zkoss.zul.Label(w.namaPegawai));
			r.appendChild(new org.zkoss.zul.Label(w.namaSatker));
			r.appendChild(new org.zkoss.zul.Label(w.alpa + " Hari"));
			r.appendChild(new org.zkoss.zul.Label(w.terlambat + " Kali"));
			r.appendChild(new org.zkoss.zul.Label(w.tidakAbsenPulang + " Kali"));
			String saran = w.alpa >= 3 ? "SP 1 / Panggilan HRD"
					: (w.alpa > 0 ? "Teguran Lisan" : "Evaluasi Disiplin Waktu");
			r.appendChild(new org.zkoss.zul.Label(saran));
			urutan++;
		}
	}

	private void renderRingkasanRiwayatKehadiran() {
		org.zkoss.zul.Panelchildren pch = createPanel("Ringkasan Riwayat Kehadiran Karyawan (Akumulasi Metrik Lengkap)",
				portal.pcBottom, "padding: 10px; background: #fff; overflow-x: auto;");
		MyGrid grid = createGrid(pch, 10, "3000px");
		addColumns(grid, "Nama Pegawai", "Satuan Kerja", "Hari Aktif", "Hari Efektif", "Σ Hadir", "Tepat Wkt",
				"Tpt Wkt Bgt", "Telat", "Plg Cepat", "Tdk Absn Plg", "Σ Pengajuan", "Alpha", "Sakit", "Izin",
				"Cuti Mmtg", "Cuti Tdk Mmtg", "Jatah Cuti Total", "Jatah Cuti Bersama", "Sisa Cuti Bisa Diambil", "Belum Absn", "Lainnya",
				"Tdk Hadir", "Tdk Hdr (NonLbr)", "Σ Jam Kerja", "Σ Jam Lembur", "Σ Jam Telat", "Σ Jam Plg Cepat",
				"Σ Jam Dtg Awal", "Σ Jam Awal (Tol)", "Σ Jam Lwt Shift");
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		rows.setParent(grid);
		for (RingkasanPegawaiHolder h : data.mapRingkasan.values()) {
			org.zkoss.zul.Row r = new org.zkoss.zul.Row();
			r.setParent(rows);
			r.appendChild(new org.zkoss.zul.Label(h.namaPegawai));
			r.appendChild(new org.zkoss.zul.Label(h.namaSatker));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.aktif)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.jumlahHariEfektif)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.masuk)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.tepatWaktu)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.tepatWaktuBanget)));
			appendStyledNumber(r, h.terlambat, "#fd7e14");
			appendStyledNumber(r, h.pulangcepat, "#fd7e14");
			appendStyledNumber(r, h.tidakAbsenPulang, "#fd7e14");
			appendStyledNumber(r, h.jumlahPengajuan, "#0dcaf0");
			appendStyledNumber(r, h.alpa, "#dc3545");
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.sakit)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.izin)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.cuti_memotong)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.cuti_tidak_memotong)));
			r.appendChild(new org.zkoss.zul.Label(h.jumlahCutiTotal + " Hari"));
			r.appendChild(new org.zkoss.zul.Label(h.jumlahCutiBersama + " Hari"));
			r.appendChild(new org.zkoss.zul.Label(h.jumlahCutiYangBisaDiambil + " Hari"));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.belum)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.lain)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.tidak_hadir)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.tidakHadirTanpaHoliday)));
			r.appendChild(new org.zkoss.zul.Label(Common.numberFormat.get().format(h.jamMasuk) + " j"));
			r.appendChild(new org.zkoss.zul.Label(Common.numberFormat.get().format(h.lemburMasuk) + " j"));
			appendStyledText(r, Common.numberFormat.get().format(h.terlambatJam) + " j", h.terlambatJam > 0, "#fd7e14");
			appendStyledText(r, Common.numberFormat.get().format(h.cepatKeluar) + " j", h.cepatKeluar > 0, "#fd7e14");
			r.appendChild(new org.zkoss.zul.Label(Common.numberFormat.get().format(h.cepatJam) + " j"));
			r.appendChild(new org.zkoss.zul.Label(Common.numberFormat.get().format(h.sebelumWaktu) + " j"));
			r.appendChild(new org.zkoss.zul.Label(Common.numberFormat.get().format(h.setelahWaktu) + " j"));
		}
	}

	private void appendStyledNumber(org.zkoss.zul.Row r, long value, String color) {
		appendStyledText(r, String.valueOf(value), value > 0, color);
	}

	private void appendStyledText(org.zkoss.zul.Row r, String value, boolean highlight, String color) {
		org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(value);
		if (highlight)
			lbl.setStyle("color:" + color + "; font-weight:bold;");
		r.appendChild(lbl);
	}

	private void renderRincianLogRiwayatKehadiran() {
		org.zkoss.zul.Panelchildren pch = createPanel("Rincian Log Riwayat Kehadiran Pegawai", portal.pcBottom, null);
		renderSortToolbar(pch);

		MyGrid grid = createGrid(pch, 10, "100%");
		addColumns(grid, "Tanggal", "Nama Pegawai", "Satuan Kerja", "Jam Masuk", "Jam Pulang", "Status", "Jam Lembur",
				"Keterangan");
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		rows.setParent(grid);

		for (StatuskehadiranKaryawanHarian skh : data.listKehadiranFiltered) {
			org.zkoss.zul.Row r = new org.zkoss.zul.Row();
			r.setParent(rows);
			String tglStr = skh.getTanggal() != null ? Common.dateFormat4.get().format(skh.getTanggal()) : "-";
			String nama = skh.getPegawai() != null ? skh.getPegawai().getNama() : "Anonim";
			String satker = (skh.getPegawai() != null && skh.getPegawai().getSatuanKerja() != null)
					? skh.getPegawai().getSatuanKerja().getNama()
					: "-";
			String jamMasuk = skh.ambilMasukjam() != null ? Common.timeFormat.get().format(skh.ambilMasukjam()) : "-";
			String jamPulang = skh.ambilPulangjam() != null ? Common.timeFormat.get().format(skh.ambilPulangjam())
					: "-";
			StatusUi statusUi = getStatusUi(skh);
			Double dLembur = getLemburValid(skh, skh.getPegawai());
			String txtLembur = dLembur > 0.01 ? Common.numberFormat.get().format(dLembur) + " Jam" : "-";

			r.appendChild(new org.zkoss.zul.Label(tglStr));
			r.appendChild(new org.zkoss.zul.Label(nama));
			r.appendChild(new org.zkoss.zul.Label(satker));
			r.appendChild(new org.zkoss.zul.Label(jamMasuk));
			r.appendChild(new org.zkoss.zul.Label(jamPulang));
			org.zkoss.zul.Label lblStatus = new org.zkoss.zul.Label(statusUi.text);
			lblStatus.setStyle("font-weight: 600; padding: 4px 8px; border-radius: 4px; background-color: "
					+ statusUi.color + "20; color: " + statusUi.color + ";");
			r.appendChild(lblStatus);
			org.zkoss.zul.Label lblLembur = new org.zkoss.zul.Label(txtLembur);
			lblLembur.setStyle(dLembur > 0.01 ? "font-weight: bold; color: #6f42c1;" : "");
			r.appendChild(lblLembur);
			skh.renderKeteranganLink(r);
		}
	}

	private void renderSortToolbar(Component parent) {
		org.zkoss.zul.Toolbar sortToolbar = new org.zkoss.zul.Toolbar();
		sortToolbar.setStyle("border:none; background:transparent; padding-bottom:10px;");
		sortToolbar.setParent(parent);
		sortToolbar.appendChild(new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Urutkan Berdasarkan: ")));

		final org.zkoss.zul.Combobox cmbSort = new org.zkoss.zul.Combobox();
		cmbSort.setReadonly(true);
		cmbSort.appendItem("Waktu (Terbaru)").setValue("waktu_desc");
		cmbSort.appendItem("Waktu (Terlama)").setValue("waktu_asc");
		cmbSort.appendItem("Nama Pegawai (A-Z)").setValue("nama_asc");
		cmbSort.appendItem("Nama Pegawai (Z-A)").setValue("nama_desc");
		cmbSort.appendItem("Jam Masuk (Terpagi)").setValue("masuk_asc");
		cmbSort.appendItem("Jam Masuk (Tersiang)").setValue("masuk_desc");
		cmbSort.appendItem("Jam Pulang (Tercepat)").setValue("pulang_asc");
		cmbSort.appendItem("Jam Pulang (Termalam)").setValue("pulang_desc");
		cmbSort.appendItem("Jam Lembur (Tertinggi)").setValue("lembur_desc");
		cmbSort.appendItem("Jam Lembur (Terendah)").setValue("lembur_asc");
		for (int i = 0; i < cmbSort.getItemCount(); i++) {
			if (cmbSort.getItemAtIndex(i).getValue().equals(currentSort)) {
				cmbSort.setSelectedIndex(i);
				break;
			}
		}
		cmbSort.setParent(sortToolbar);
		cmbSort.addEventListener("onSelect", new EventListener() {
			public void onEvent(Event event) throws Exception {
				String nextSort = cmbSort.getSelectedItem().getValue().toString();
				reloadHandler.reload(dbMulai.getValue(), dbSampai.getValue(), getSelectedActiveDays(),
						(ais.database.model.rab.SatuanKerja) cbSatker.getAttribute("satuanKerja"), nextSort);
			}
		});
	}

	private StatusUi getStatusUi(StatuskehadiranKaryawanHarian skh) {
		StatusUi ui = new StatusUi();
		ui.text = "Tepat Waktu";
		ui.color = "#198754";
		if (isLogHariLibur(skh)) {
			if (skh.ambilMasukjam() != null) {
				ui.text = "Masuk Hari Libur";
				ui.color = "#0d6efd";
			} else {
				ui.text = "Libur";
				ui.color = "#6c757d";
			}
			return ui;
		}
		if (skh.getStatusabsensi() != null
				&& !skh.getStatusabsensi().getId().equals(ais.common.ConstantValues.MASUK.getId())) {
			ui.text = skh.getStatusabsensi().getNama();
			ui.color = skh.getStatusabsensi().getId().equals(ais.common.ConstantValues.TIDAK_ADA_ALASAN.getId())
					? "#dc3545"
					: "#0dcaf0";
		} else if (Boolean.TRUE.equals(skh.getDatangTerlambat())) {
			ui.text = "Terlambat";
			ui.color = "#fd7e14";
		} else if (Boolean.TRUE.equals(skh.getPulangCepat())) {
			ui.text = "Pulang Cepat";
			ui.color = "#0d6efd";
		}
		return ui;
	}

	private void renderKinerjaKedisiplinanSatker() {
		org.zkoss.zul.Panelchildren pch = createPanel("Kinerja Kedisiplinan per Satuan Kerja (Unit/Departemen)",
				portal.pcBottom, null);
		MyGrid grid = createGrid(pch, 10, "100%");
		addColumns(grid, "Satuan Kerja", "Total Hari/Log", "Hadir (Tepat & Telat)", "Sering Telat", "Total Alpha",
				"Rasio Kehadiran", "Rasio Ketidakhadiran", "Σ Jam Lembur");
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		rows.setParent(grid);
		for (RingkasanSatkerHolder sh : data.mapSatker.values()) {
			org.zkoss.zul.Row r = new org.zkoss.zul.Row();
			r.setParent(rows);
			r.appendChild(new org.zkoss.zul.Label(sh.namaSatker));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(sh.totalLog)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(sh.totalHadir)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(sh.totalTelat)));
			appendStyledNumber(r, sh.totalAlpha, "#dc3545");
			double rasio = sh.totalLog == 0 ? 0.0 : (sh.totalHadir * 100.0 / sh.totalLog);
			double rasioTidakHadir = sh.totalLog == 0 ? 0.0 : (sh.totalAlpha * 100.0 / sh.totalLog);
			r.appendChild(new org.zkoss.zul.Label(Common.numberFormat.get().format(rasio) + " %"));
			r.appendChild(new org.zkoss.zul.Label(Common.numberFormat.get().format(rasioTidakHadir) + " %"));
			r.appendChild(new org.zkoss.zul.Label(Common.numberFormat.get().format(sh.totalJamLembur) + " Jam"));
		}
	}

	private void renderBebanLemburPegawai() {
		String title = "Beban Lembur Pegawai";
		if (data != null && data.bebanLemburPegawaiMax != -1) {
			title += " (Maks. " + data.bebanLemburPegawaiMax + " Jam/Pegawai)";
		}
		org.zkoss.zul.Panelchildren pch = createPanel(title, portal.pcBottom, null);
		List<RingkasanPegawaiHolder> list = new java.util.ArrayList<RingkasanPegawaiHolder>(data.mapRingkasan.values());
		java.util.Collections.sort(list, new java.util.Comparator<RingkasanPegawaiHolder>() {
			public int compare(RingkasanPegawaiHolder h1, RingkasanPegawaiHolder h2) {
				return Double.compare(h2.lemburMasuk, h1.lemburMasuk);
			}
		});
		MyGrid grid = createGrid(pch, 10, "100%");
		addColumns(grid, "Nama Pegawai", "Satuan Kerja", "Total Jam Lembur", "Status");
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		rows.setParent(grid);
		for (RingkasanPegawaiHolder hw : list) {
			if (hw.lemburMasuk <= 0)
				continue;
			org.zkoss.zul.Row r = new org.zkoss.zul.Row();
			r.setParent(rows);
			r.appendChild(new org.zkoss.zul.Label(hw.namaPegawai));
			r.appendChild(new org.zkoss.zul.Label(hw.namaSatker));
			org.zkoss.zul.Label lblLembur = new org.zkoss.zul.Label(
					Common.numberFormat.get().format(hw.lemburMasuk) + " Jam");
			lblLembur.setStyle("font-weight: bold; color: #dc3545;");
			r.appendChild(lblLembur);
			r.appendChild(new org.zkoss.zul.Label(hw.lemburMasuk > 20 ? "Kritis / Overwork" : "Waspada"));
		}
	}

	private void renderGrafikDistribusiAlasanKetidakhadiran() {
		org.zkoss.zul.Panelchildren pch = createPanel("Grafik Distribusi Alasan Ketidakhadiran", portal.pcBottom,
				"padding:14px; background:#fff;");
		long hrdSakit = 0, hrdIzin = 0, hrdAlpha = 0, hrdCuti = 0;
		for (RingkasanPegawaiHolder h : data.mapRingkasan.values()) {
			hrdSakit += h.sakit;
			hrdIzin += h.izin;
			hrdCuti += h.cuti_memotong + h.cuti_tidak_memotong;
			hrdAlpha += h.alpa;
		}
		String[][] values = new String[][] { { "Sakit", String.valueOf(hrdSakit), "#2563eb" },
				{ "Izin", String.valueOf(hrdIzin), "#7c3aed" }, { "Cuti", String.valueOf(hrdCuti), "#16a34a" },
				{ "Alpha", String.valueOf(hrdAlpha), "#dc2626" } };
		appendHtml(pch, buildHorizontalBarsHtml(values,
				"Distribusi ini memperlihatkan alasan ketidakhadiran yang paling sering muncul, sehingga HRD dapat membedakan kasus yang wajar seperti sakit/cuti dengan kasus yang perlu pembinaan seperti alpha."));
	}

	private void renderGrafikTrendBebanKerjaHarian() {
		org.zkoss.zul.Panelchildren pch = createPanel("Grafik Trend Beban Kerja Harian (Jam)", portal.pcBottom,
				"padding:14px; background:#fff;");
		appendHtml(pch, buildTrendBebanKerjaHtml(data.dataTrenHarian));
	}

	private void renderGrafikAnalisaDetailKehadiran() {
		org.zkoss.zul.Panelchildren pchChart = createPanel("Grafik Analisa Detail Kondisi Kehadiran", portal.pcBottom,
				"padding:14px; background:#fff;");
		appendHtml(pchChart, buildStatusKondisiHtml());

		org.zkoss.zul.Panelchildren pchTable = createPanel("Grafik Rekapitulasi Kondisi Kehadiran Pegawai", portal.pcBottom,
				null);
		MyGrid grid = createGrid(pchTable, 10, "100%");
		addColumns(grid, "Kondisi / Status", "Frekuensi (Total Hari)");
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		rows.setParent(grid);
		for (String cat : data.statusKondisiCount.keySet()) {
			org.zkoss.zul.Row r = new org.zkoss.zul.Row();
			r.setParent(rows);
			org.zkoss.zul.Label lblStatus = new org.zkoss.zul.Label(cat);
			lblStatus.setStyle("padding:4px 10px; border-radius:999px; font-weight:bold; background:#f1f5f9; color:#0f172a; border:1px solid #e2e8f0;");
			r.appendChild(lblStatus);
			org.zkoss.zul.Label lblTotal = new org.zkoss.zul.Label(data.statusKondisiCount.get(cat) + " Kali");
			lblTotal.setStyle("font-weight: bold;");
			r.appendChild(lblTotal);
		}
	}

	private void renderPeringkatPegawaiTerajin() {
		org.zkoss.zul.Panelchildren pch = createPanel("Peringkat Pegawai Terajin (Berdasarkan Skor Kedisiplinan Jam)",
				portal.pcBottom, null);
		List<RankWrapper> rankList = new java.util.ArrayList<RankWrapper>();
		for (RingkasanPegawaiHolder h : data.mapRingkasan.values())
			rankList.add(new RankWrapper(h));
		java.util.Collections.sort(rankList, new java.util.Comparator<RankWrapper>() {
			public int compare(RankWrapper r1, RankWrapper r2) {
				return Double.compare(r2.score, r1.score);
			}
		});
		MyGrid grid = createGrid(pch, 10, "100%");
		addColumns(grid, "Peringkat", "Nama Pegawai", "Skor Rajin (Jam)", "Status");
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		rows.setParent(grid);
		int rank = 1;
		for (RankWrapper rw : rankList) {
			org.zkoss.zul.Row r = new org.zkoss.zul.Row();
			r.setParent(rows);
			r.appendChild(new org.zkoss.zul.Label("#" + rank));
			r.appendChild(new org.zkoss.zul.Label(rw.holder.namaPegawai));
			org.zkoss.zul.Label lblScore = new org.zkoss.zul.Label(Common.numberFormat.get().format(rw.score));
			lblScore.setStyle("font-weight:bold; color:" + (rw.score >= 0 ? "#198754" : "#dc3545"));
			r.appendChild(lblScore);
			String kualitatif = rw.score >= 10 ? "Sangat Rajin" : (rw.score >= 0 ? "Rajin" : "Perlu Kedisiplinan");
			r.appendChild(new org.zkoss.zul.Label(kualitatif));
			rank++;
		}
	}

	private void renderTopPegawaiPalingDisiplin() {
		org.zkoss.zul.Panelchildren pch = createPanel("Top Pegawai Paling Disiplin", portal.pcBottom, null);
		List<RingkasanPegawaiHolder> list = new java.util.ArrayList<RingkasanPegawaiHolder>(data.mapRingkasan.values());
		java.util.Collections.sort(list, new java.util.Comparator<RingkasanPegawaiHolder>() {
			public int compare(RingkasanPegawaiHolder a, RingkasanPegawaiHolder b) {
				long scoreA = (a.tepatWaktu + a.tepatWaktuBanget) - (a.terlambat + a.alpa);
				long scoreB = (b.tepatWaktu + b.tepatWaktuBanget) - (b.terlambat + b.alpa);
				return Long.compare(scoreB, scoreA);
			}
		});
		MyGrid grid = createGrid(pch, 10, "100%");
		addColumns(grid, "Rank", "Nama Pegawai", "Satker", "Tepat Waktu", "Alpha", "Skor");
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		rows.setParent(grid);
		int rankDis = 1;
		for (RingkasanPegawaiHolder h : list) {
			if (rankDis > 10)
				break;
			long score = (h.tepatWaktu + h.tepatWaktuBanget) - (h.terlambat + h.alpa);
			org.zkoss.zul.Row r = new org.zkoss.zul.Row();
			r.setParent(rows);
			r.appendChild(new org.zkoss.zul.Label("#" + rankDis));
			r.appendChild(new org.zkoss.zul.Label(h.namaPegawai));
			r.appendChild(new org.zkoss.zul.Label(h.namaSatker));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.tepatWaktu)));
			r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.alpa)));
			org.zkoss.zul.Label lblScore = new org.zkoss.zul.Label(String.valueOf(score));
			lblScore.setStyle("font-weight:bold; color:#198754;");
			r.appendChild(lblScore);
			rankDis++;
		}
	}

	private void renderPegawaiTidakAbsenPulang() {
		org.zkoss.zul.Panelchildren pch = createPanel("Pegawai Tidak Absen Pulang", portal.pcBottom, null);
		MyGrid grid = createGrid(pch, 10, "100%");
		addColumns(grid, "Nama Pegawai", "Satker", "Jumlah", "Status");
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		rows.setParent(grid);
		for (RingkasanPegawaiHolder h : data.mapRingkasan.values()) {
			if (h.tidakAbsenPulang <= 0)
				continue;
			org.zkoss.zul.Row r = new org.zkoss.zul.Row();
			r.setParent(rows);
			r.appendChild(new org.zkoss.zul.Label(h.namaPegawai));
			r.appendChild(new org.zkoss.zul.Label(h.namaSatker));
			org.zkoss.zul.Label lblJumlah = new org.zkoss.zul.Label(String.valueOf(h.tidakAbsenPulang));
			lblJumlah.setStyle("font-weight:bold; color:#dc3545;");
			r.appendChild(lblJumlah);
			r.appendChild(new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Perlu Klarifikasi")));
		}
	}

	private void renderStatistikProduktivitasKehadiran() {
		org.zkoss.zul.Panelchildren pch = createPanel("Statistik Produktivitas Kehadiran", portal.pcBottom, null);
		org.zkoss.zul.Vbox vbProd = new org.zkoss.zul.Vbox();
		vbProd.setSpacing("10px");
		vbProd.setParent(pch);

		double rataJamKerja = 0.0;
		double rataJamLembur = 0.0;
		if (!data.mapRingkasan.isEmpty()) {
			for (RingkasanPegawaiHolder h : data.mapRingkasan.values()) {
				rataJamKerja += h.jamMasuk;
				rataJamLembur += h.lemburMasuk;
			}
			rataJamKerja = rataJamKerja / data.mapRingkasan.size();
			rataJamLembur = rataJamLembur / data.mapRingkasan.size();
		}
		vbProd.appendChild(new org.zkoss.zul.Label(
				"Rata-rata Jam Kerja Pegawai : " + Common.numberFormat.get().format(rataJamKerja) + " Jam"));
		vbProd.appendChild(new org.zkoss.zul.Label(
				"Rata-rata Jam Lembur Pegawai : " + Common.numberFormat.get().format(rataJamLembur) + " Jam"));
		vbProd.appendChild(new org.zkoss.zul.Label("Total Pegawai Dianalisa : " + data.mapRingkasan.size()));
		vbProd.appendChild(new org.zkoss.zul.Label("Total Satuan Kerja Aktif : " + data.mapSatker.size()));
	}

	@SuppressWarnings("deprecation")

	private void renderRadarKesehatanKehadiranHtml() {
		org.zkoss.zul.Panelchildren pch = createPanel("Radar Kesehatan Kehadiran", portal.pcBottom, "padding:14px; background:#fff;");
		long totalHari = 0L;
		long hadir = 0L;
		long tepat = 0L;
		long risiko = 0L;
		double jamEfektif = 0.0;
		for (RingkasanPegawaiHolder h : data.mapRingkasan.values()) {
			totalHari += h.aktif;
			hadir += h.masuk;
			tepat += h.tepatWaktu + h.tepatWaktuBanget;
			risiko += h.alpa + h.terlambat + h.tidakAbsenPulang + h.pulangcepat;
			jamEfektif += h.jamMasuk - h.terlambatJam - h.cepatKeluar;
		}
		int pHadir = percent(hadir, totalHari);
		int pTepat = percent(tepat, totalHari);
		int pDisiplin = 100 - percent(risiko, totalHari <= 0 ? 1 : totalHari);
		int pJam = jamEfektif <= 0.0 ? 0 : (int) Math.min(100.0, Math.round(jamEfektif / Math.max(1, data.mapRingkasan.size())));
		String html = "<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(260px,1fr)); gap:14px; align-items:center;'>"
				+ "<div style='padding:18px; border-radius:18px; background:#f8fafc; border:1px solid #e2e8f0; text-align:center;'>"
				+ "<div style='margin:auto; width:190px; height:190px; border-radius:999px; position:relative; background:conic-gradient(#16a34a 0 "
				+ pDisiplin + "%,#e5e7eb " + pDisiplin + "% 100%); box-shadow:inset 0 0 0 20px #ffffff,0 12px 24px rgba(15,23,42,.08);'>"
				+ "<div style='position:absolute; inset:42px; border-radius:999px; background:conic-gradient(#2563eb 0 "
				+ pHadir + "%,#e5e7eb " + pHadir + "% 100%);'></div>"
				+ "<div style='position:absolute; inset:72px; border-radius:999px; background:#fff; display:flex; align-items:center; justify-content:center; font-size:22px; font-weight:900; color:#0f172a;'>"
				+ pDisiplin + "%</div></div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:10px;'>Lingkar luar menunjukkan disiplin umum, lingkar dalam menunjukkan rasio hadir.</div></div>"
				+ "<div style='display:flex; flex-direction:column; gap:9px;'>"
				+ buildMiniGaugeHtml("Rasio Hadir", pHadir, "Pegawai hadir dibanding hari aktif.", "#2563eb")
				+ buildMiniGaugeHtml("Ketepatan Waktu", pTepat, "Kehadiran tepat waktu dibanding hari aktif.", "#16a34a")
				+ buildMiniGaugeHtml("Disiplin Umum", pDisiplin, "Semakin tinggi berarti semakin sedikit alpha, terlambat, pulang cepat, dan tidak absen pulang.", "#0f766e")
				+ buildMiniGaugeHtml("Jam Efektif", pJam, "Gambaran sederhana kecukupan jam kerja efektif rata-rata.", "#7c3aed")
				+ "</div></div>";
		appendHtml(pch, html);
	}

	private void appendHtml(Component parent, String html) {
		org.zkoss.zul.Html h = new org.zkoss.zul.Html(html == null ? "" : html);
		h.setParent(parent);
	}

	private String buildHorizontalBarsHtml(String[][] values, String intro) {
		long max = 1L;
		long total = 0L;
		for (int i = 0; values != null && i < values.length; i++) {
			long v = toLong(values[i][1]);
			if (v > max) {
				max = v;
			}
			total += v;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:12px;color:#64748b;line-height:1.6;margin-bottom:12px;'>")
				.append(escapeHtml(intro)).append("</div>");
		sb.append("<div style='display:flex;flex-direction:column;gap:10px;'>");
		for (int i = 0; values != null && i < values.length; i++) {
			String label = values[i][0];
			long v = toLong(values[i][1]);
			String color = values[i][2];
			long pct = Math.round((v * 100.0d) / (max <= 0 ? 1 : max));
			long share = Math.round((v * 100.0d) / (total <= 0 ? 1 : total));
			sb.append("<div style='display:grid; grid-template-columns:130px 1fr 90px; gap:10px; align-items:center;'>")
					.append("<div style='font-size:12px;font-weight:800;color:#0f172a;'>").append(escapeHtml(label)).append("</div>")
					.append("<div style='height:16px;background:#e5e7eb;border-radius:999px;overflow:hidden;'>")
					.append("<div style='height:16px;width:").append(pct).append("%;background:").append(color).append(";border-radius:999px;'></div></div>")
					.append("<div style='font-size:12px;font-weight:900;color:").append(color).append(";text-align:right;'>")
					.append(v).append(" (±").append(share).append("%)</div></div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String buildTrendBebanKerjaHtml(java.util.Map<String, double[]> dataTren) {
		if (dataTren == null || dataTren.isEmpty()) {
			return "<div style='padding:14px;border-radius:14px;background:#f8fafc;border:1px solid #e2e8f0;color:#64748b;'>Belum ada data trend beban kerja untuk filter ini.</div>";
		}
		StringBuilder sb = new StringBuilder();
		double max = 1.0;
		for (double[] v : dataTren.values()) {
			if (v == null) {
				continue;
			}
			for (int i = 0; i < v.length; i++) {
				if (v[i] > max) {
					max = v[i];
				}
			}
		}
		sb.append("<div style='font-size:12px;color:#64748b;line-height:1.6;margin-bottom:12px;'>Trend ini memperlihatkan perubahan jam kerja, lembur, keterlambatan, pulang cepat, datang awal, dan toleransi shift dari hari ke hari. Ketinggian bar membantu melihat hari yang bebannya paling besar.</div>");
		sb.append("<div style='overflow:auto; padding-bottom:6px;'><div style='display:flex; gap:10px; align-items:flex-end; min-height:210px;'>");
		int count = 0;
		for (String key : dataTren.keySet()) {
			if (count >= 45) {
				break;
			}
			double[] v = dataTren.get(key);
			if (v == null) {
				continue;
			}
			double jamKerja = v.length > 0 ? v[0] : 0.0;
			double lembur = v.length > 1 ? v[1] : 0.0;
			double telat = v.length > 2 ? v[2] : 0.0;
			double pulangCepat = v.length > 3 ? v[3] : 0.0;
			int h1 = 8 + (int) Math.round((jamKerja * 125.0d) / max);
			int h2 = 8 + (int) Math.round((lembur * 125.0d) / max);
			int h3 = 8 + (int) Math.round((telat * 125.0d) / max);
			int h4 = 8 + (int) Math.round((pulangCepat * 125.0d) / max);
			sb.append("<div style='min-width:76px;text-align:center;'>")
					.append("<div style='height:150px;display:flex;align-items:flex-end;justify-content:center;gap:3px;'>")
					.append("<div title='Jam kerja: ").append(Common.numberFormat.get().format(jamKerja)).append("' style='width:12px;height:").append(h1).append("px;background:#2563eb;border-radius:8px 8px 3px 3px;'></div>")
					.append("<div title='Lembur: ").append(Common.numberFormat.get().format(lembur)).append("' style='width:12px;height:").append(h2).append("px;background:#7c3aed;border-radius:8px 8px 3px 3px;'></div>")
					.append("<div title='Telat: ").append(Common.numberFormat.get().format(telat)).append("' style='width:12px;height:").append(h3).append("px;background:#f59e0b;border-radius:8px 8px 3px 3px;'></div>")
					.append("<div title='Pulang cepat: ").append(Common.numberFormat.get().format(pulangCepat)).append("' style='width:12px;height:").append(h4).append("px;background:#dc2626;border-radius:8px 8px 3px 3px;'></div>")
					.append("</div><div style='font-size:10px;color:#64748b;margin-top:5px;white-space:nowrap;'>")
					.append(escapeHtml(key)).append("</div></div>");
			count++;
		}
		sb.append("</div></div>");
		sb.append("<div style='display:flex;gap:8px;flex-wrap:wrap;margin-top:10px;font-size:11px;color:#475569;'>")
				.append(legendHtml("#2563eb", "Jam kerja"))
				.append(legendHtml("#7c3aed", "Lembur"))
				.append(legendHtml("#f59e0b", "Telat"))
				.append(legendHtml("#dc2626", "Pulang cepat"))
				.append("</div>");
		return sb.toString();
	}

	private String buildStatusKondisiHtml() {
		long max = 1L;
		long total = 0L;
		for (String key : data.statusKondisiCount.keySet()) {
			long v = data.statusKondisiCount.get(key) == null ? 0L : data.statusKondisiCount.get(key).longValue();
			if (v > max) {
				max = v;
			}
			total += v;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:12px;color:#64748b;line-height:1.6;margin-bottom:12px;'>Sebaran kondisi kehadiran membantu membedakan status yang dominan, seperti hadir, terlambat, cuti, izin, sakit, alpha, atau tidak absen pulang.</div>");
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:12px;'>");
		for (String key : data.statusKondisiCount.keySet()) {
			long v = data.statusKondisiCount.get(key) == null ? 0L : data.statusKondisiCount.get(key).longValue();
			long share = Math.round((v * 100.0d) / (total <= 0 ? 1 : total));
			String color = data.statusKondisiColor.get(key) == null ? "#2563eb" : data.statusKondisiColor.get(key);
			sb.append("<div style='padding:12px;border-radius:16px;background:#f8fafc;border:1px solid #e2e8f0;'>")
					.append("<div style='font-size:12px;font-weight:900;color:#0f172a;'>").append(escapeHtml(key)).append("</div>")
					.append("<div style='font-size:24px;font-weight:900;color:").append(color).append(";margin-top:8px;'>").append(v).append("</div>")
					.append("<div style='height:9px;background:#e5e7eb;border-radius:999px;overflow:hidden;margin-top:8px;'>")
					.append("<div style='height:9px;width:").append(share).append("%;background:").append(color).append(";border-radius:999px;'></div></div>")
					.append("<div style='font-size:10px;color:#64748b;margin-top:5px;'>±").append(share).append("% dari total kondisi.</div></div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String buildMiniGaugeHtml(String label, int value, String desc, String color) {
		if (value < 0) {
			value = 0;
		}
		if (value > 100) {
			value = 100;
		}
		return "<div><div style='display:flex;justify-content:space-between;font-size:11px;font-weight:900;color:#0f172a;'><span>"
				+ escapeHtml(label) + "</span><span>" + value + "%</span></div>"
				+ "<div style='height:10px;border-radius:999px;background:#e2e8f0;overflow:hidden;margin-top:4px;'>"
				+ "<div style='height:10px;width:" + value + "%;background:" + color + ";border-radius:999px;'></div></div>"
				+ "<div style='font-size:10px;color:#64748b;margin-top:3px;'>" + escapeHtml(desc) + "</div></div>";
	}

	private String legendHtml(String color, String text) {
		return "<span style='display:inline-flex;align-items:center;gap:5px;padding:5px 9px;border-radius:999px;background:#f8fafc;border:1px solid #e2e8f0;'>"
				+ "<span style='width:10px;height:10px;border-radius:999px;background:" + color + ";display:inline-block;'></span>"
				+ escapeHtml(text) + "</span>";
	}

	private int percent(long value, long total) {
		if (total <= 0L) {
			return 0;
		}
		int p = (int) Math.round((value * 100.0d) / total);
		if (p < 0) {
			return 0;
		}
		if (p > 100) {
			return 100;
		}
		return p;
	}

	private long toLong(String value) {
		try {
			return Long.parseLong(value);
		} catch (Exception e) {
			return 0L;
		}
	}


	public static interface ProgressHandler {
		void update(String message, int percent);
	}

	private static class PortalRefs {
		ais.ui.util.MyPortalchildren pcTop;
		ais.ui.util.MyPortalchildren pcLeft;
		ais.ui.util.MyPortalchildren pcRight;
		ais.ui.util.MyPortalchildren pcBottom;
	}

	static class DashboardData {
		ProgressHandler progressHandler;
		List<StatuskehadiranKaryawanHarian> listKehadiranFiltered = new java.util.ArrayList<StatuskehadiranKaryawanHarian>();
		int totalLogGlobal = 0, totalHadirTepatWaktuGlobal = 0, totalHadirTerlambatGlobal = 0;
		int totalPulangCepatGlobal = 0, totalCutiIzinGlobal = 0, totalCutiMemotongGlobal = 0,
				totalCutiTidakMemotongGlobal = 0, totalAlphaGlobal = 0, totalLemburGlobal = 0;
		double totalJamLemburGlobal = 0.0;
		int bebanLemburPegawaiMax = -1;
		java.util.Set<String> cutiIzinGlobalKeys = new java.util.HashSet<String>();
		java.util.Set<String> cutiMemotongGlobalKeys = new java.util.HashSet<String>();
		java.util.Set<String> cutiTidakMemotongGlobalKeys = new java.util.HashSet<String>();
		java.util.Map<String, double[]> dataTrenHarian = new java.util.TreeMap<String, double[]>();
		java.util.Map<String, Integer> statusKondisiCount = new java.util.LinkedHashMap<String, Integer>();
		java.util.Map<String, String> statusKondisiBg = new java.util.HashMap<String, String>();
		java.util.Map<String, String> statusKondisiColor = new java.util.HashMap<String, String>();
		long totalTidakAbsenPulangGlobal = 0;
		long totalMasukHariLiburGlobal = 0;
		long totalHariLiburGlobal = 0;
		java.util.Set<String> hariLiburLogKeys = new java.util.HashSet<String>();
		java.util.Map<String, RingkasanPegawaiHolder> mapRingkasan = new java.util.LinkedHashMap<String, RingkasanPegawaiHolder>();
		java.util.Map<String, RingkasanSatkerHolder> mapSatker = new java.util.TreeMap<String, RingkasanSatkerHolder>();
	}

	static class RingkasanPegawaiHolder {
		String namaPegawai;
		String namaSatker;
		long aktif = 0L, jumlahHariEfektif = 0L, tidak_hadir = 0L, tidakHadirTanpaHoliday = 0L,
				cuti_memotong = 0L, cuti_tidak_memotong = 0L;
		long masuk = 0L, alpa = 0L, sakit = 0L, izin = 0L, belum = 0L, lain = 0L;
		long tidakAbsenPulang = 0L, tepatWaktu = 0L, tepatWaktuBanget = 0L, terlambat = 0L, pulangcepat = 0L;
		double jamMasuk = 0.0, lemburMasuk = 0.0, terlambatJam = 0.0, cepatKeluar = 0.0, cepatJam = 0.0,
				sebelumWaktu = 0.0, setelahWaktu = 0.0;
		int jumlahCutiTotal = 0, jumlahCutiBersama = 0, jumlahCutiYangBisaDiambil = 0, jumlahPengajuan = 0;
		java.util.Set<String> cutiIzinKeys = new java.util.HashSet<String>();
	}

	private static class RingkasanSatkerHolder {
		String namaSatker;
		int totalLog = 0, totalHadir = 0, totalTelat = 0, totalAlpha = 0;
		double totalJamLembur = 0.0;
	}

	private static class KondisiInfo {
		String status;
		String bgColor;
		String textColor;
	}

	private static class StatusUi {
		String text;
		String color;
	}

	private static class RankWrapper {
		RingkasanPegawaiHolder holder;
		double score;

		RankWrapper(RingkasanPegawaiHolder h) {
			this.holder = h;
			this.score = (h.cepatJam + h.setelahWaktu) - (h.terlambatJam + h.cepatKeluar);
		}
	}
}
