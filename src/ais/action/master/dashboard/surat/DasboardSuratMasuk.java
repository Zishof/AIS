package ais.action.master.dashboard.surat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.MainAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sirkulasisurat.helper.AmbilDataPeminjamSuratBanbox;
import ais.action.master.surat.SuratMasukAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Pejabat;
import ais.database.model.sirkulasisurat.KembaliSuratItemDetail;
import ais.database.model.sirkulasisurat.PeminjamSurat;
import ais.database.model.sirkulasisurat.PeminjamanSuratItemDetail;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;
import ais.database.model.surat.KlasifikasiSuratMasuk;
import ais.database.model.surat.LokerSurat;
import ais.database.model.surat.OpsiSuratMasukValue;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.WaktuUtil;

public class DasboardSuratMasuk extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Center center;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private int width = 750;
	private int height = 100;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Grid grid;
	private Combobox loker;

	private Integer desktopHeight = 11000;
	private MyDatebox start;
	private MyDatebox end;
	private String tipe;
	private Date mulai;
	private Date sampai = WaktuUtil.getDate();

	private static final class DisposisiChip {
		int nomor;
		String label;
		String status;
		String warna;
		String latar;
		String tooltip;
	}

	private String buatHtmlDisposisiBergrup(List<AlurPersetujuanSuratMasukStatus> statusList) {
		if (statusList == null || statusList.isEmpty()) {
			return "<div style='font-size:11px;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;"
					+ "border-radius:8px;padding:8px 10px;'>Belum ada disposisi.</div>";
		}

		Map<String, List<DisposisiChip>> perGrup = new LinkedHashMap<String, List<DisposisiChip>>();
		int nomor = 1;
		String prevLabel = null;
		Date prevWaktu = null;
		for (AlurPersetujuanSuratMasukStatus status : statusList) {
			JenisJabatan jenisJabatan = jenisJabatanDariStatus(status);
			if (jenisJabatan == null && (status == null || status.getAlurPersetujuanSuratMasuk() == null)) {
				continue;
			}

			String grup = normalisasiGrup(jenisJabatan == null ? null : jenisJabatan.getGrup());

			List<DisposisiChip> chips = perGrup.get(grup);
			if (chips == null) {
				chips = new ArrayList<DisposisiChip>();
				perGrup.put(grup, chips);
			}

			DisposisiChip chip = new DisposisiChip();
			chip.nomor = nomor;
			chip.label = labelDisposisi(status, jenisJabatan);
			isiStatusChip(chip, status);
			chip.tooltip = tooltipDisposisi(status, chip.label, chip.status, prevLabel, prevWaktu);
			chips.add(chip);
			nomor++;
			prevLabel = chip.label;
			prevWaktu = waktuDisposisi(status);
		}

		if (perGrup.isEmpty()) {
			return "<div style='font-size:11px;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;"
					+ "border-radius:8px;padding:8px 10px;'>Belum ada disposisi.</div>";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin:8px 0 6px 0;font-size:11px;color:#0f172a;background:#fff;box-sizing:border-box;'>");
		List<String> urutanGrup = urutanGrupDariSetup(perGrup);
		for (String grup : urutanGrup) {
			List<DisposisiChip> chips = perGrup.get(grup);
			if (chips == null || chips.isEmpty()) {
				continue;
			}
			sb.append("<div style='position:relative;margin:12px 0 14px 0;border:1px solid #d7dce7;"
					+ "border-radius:8px;background:#fff;padding:22px 18px 13px 18px;"
					+ "box-shadow:0 1px 4px rgba(15,23,42,.06);box-sizing:border-box;'>");
			sb.append("<span style='position:absolute;top:-10px;left:12px;background:#1f4b99;color:#fff;"
					+ "border-radius:4px;padding:4px 12px;font-size:10px;font-weight:800;line-height:1;'>")
					.append(ais.ui.util.DashboardUiKit.esc(labelGrupDisposisi(grup))).append("</span>");
			sb.append("<div style='display:grid;grid-template-columns:repeat(3,minmax(180px,1fr));"
					+ "gap:10px 28px;align-items:center;'>");
			for (DisposisiChip chip : chips) {
				sb.append("<span title='").append(ais.ui.util.DashboardUiKit.esc(chip.tooltip))
						.append("' style='display:inline-flex;align-items:center;gap:7px;max-width:100%;"
								+ "min-height:22px;padding:2px 9px 2px 3px;border-radius:999px;background:")
						.append(chip.latar).append(";border:1px solid ").append(chip.warna)
						.append("33;color:#0f172a;box-sizing:border-box;white-space:nowrap;overflow:hidden;'>");
				sb.append("<span style='flex:0 0 auto;width:18px;height:18px;border-radius:999px;background:")
						.append(chip.warna)
						.append(";color:#fff;font-size:10px;font-weight:900;display:inline-flex;align-items:center;"
								+ "justify-content:center;'>")
						.append(chip.nomor).append("</span>");
				sb.append("<span style='font-size:10px;font-weight:800;overflow:hidden;text-overflow:ellipsis;'>")
						.append(ais.ui.util.DashboardUiKit.esc(chip.label)).append("</span>");
				sb.append("<span style='font-size:9px;font-weight:800;color:").append(chip.warna)
						.append(";background:#fff;border-radius:999px;padding:1px 6px;'>")
						.append(ais.ui.util.DashboardUiKit.esc(chip.status)).append("</span>");
				sb.append("</span>");
			}
			sb.append("</div></div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private List<String> urutanGrupDariSetup(Map<String, List<DisposisiChip>> perGrup) {
		List<String> urutan = new ArrayList<String>();
		if (perGrup == null || perGrup.isEmpty()) {
			return urutan;
		}

		try {
			List<JenisJabatan> setup = HibernateUtil.currentSession().createCriteria(JenisJabatan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")).list();
			for (JenisJabatan jenisJabatan : setup) {
				if (jenisJabatan == null) {
					continue;
				}
				String grup = normalisasiGrup(jenisJabatan.getGrup());
				if (perGrup.containsKey(grup) && !urutan.contains(grup)) {
					urutan.add(grup);
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/dashboard/surat/DasboardSuratMasuk.java:urutanGrupDariSetup");
		}

		for (String grup : perGrup.keySet()) {
			if (!urutan.contains(grup)) {
				urutan.add(grup);
			}
		}
		urutkanGrupBerdasarkanNomor(urutan);
		return urutan;
	}

	private void urutkanGrupBerdasarkanNomor(List<String> urutan) {
		java.util.Collections.sort(urutan, new java.util.Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				int na = nomorAwalGrup(a);
				int nb = nomorAwalGrup(b);
				if (na != nb) {
					return na < nb ? -1 : 1;
				}
				return normalisasiGrup(a).compareToIgnoreCase(normalisasiGrup(b));
			}
		});
	}

	private int nomorAwalGrup(String grup) {
		String nilai = normalisasiGrup(grup);
		int mulai = 0;
		while (mulai < nilai.length() && Character.isWhitespace(nilai.charAt(mulai))) {
			mulai++;
		}
		int selesai = mulai;
		while (selesai < nilai.length() && Character.isDigit(nilai.charAt(selesai))) {
			selesai++;
		}
		if (selesai == mulai) {
			return Integer.MAX_VALUE;
		}
		try {
			return Integer.parseInt(nilai.substring(mulai, selesai));
		} catch (Exception e) {
			return Integer.MAX_VALUE;
		}
	}

	private JenisJabatan jenisJabatanDariStatus(AlurPersetujuanSuratMasukStatus status) {
		if (status == null) {
			return null;
		}
		if (status.getJenisJabatan() != null) {
			return status.getJenisJabatan();
		}
		if (status.getAlurPersetujuanSuratMasuk() != null) {
			return status.getAlurPersetujuanSuratMasuk().getJenisJabatan();
		}
		return null;
	}

	private String normalisasiGrup(String grup) {
		if (grup == null || grup.trim().isEmpty()) {
			return "Pejabat";
		}
		return grup.trim();
	}

	private String labelGrupDisposisi(String grup) {
		return normalisasiGrup(grup);
	}

	private String labelDisposisi(AlurPersetujuanSuratMasukStatus status, JenisJabatan jenisJabatan) {
		if (jenisJabatan != null && jenisJabatan.getNama() != null && !jenisJabatan.getNama().trim().isEmpty()) {
			return jenisJabatan.getNama();
		}
		if (status != null && status.getAlurPersetujuanSuratMasuk() != null) {
			return String.valueOf(status.getAlurPersetujuanSuratMasuk());
		}
		return "Disposisi";
	}

	private void isiStatusChip(DisposisiChip chip, AlurPersetujuanSuratMasukStatus status) {
		if (status != null && Boolean.TRUE.equals(status.getDisetujui())) {
			chip.status = "Disetujui";
			chip.warna = "#16a34a";
			chip.latar = "#dcfce7";
		} else if (status != null && Boolean.TRUE.equals(status.getDitolak())) {
			chip.status = "Ditolak";
			chip.warna = "#dc2626";
			chip.latar = "#fee2e2";
		} else {
			chip.status = "Menunggu Persetujuan";
			chip.warna = "#f59e0b";
			chip.latar = "#fef3c7";
		}
	}

	private Date waktuDisposisi(AlurPersetujuanSuratMasukStatus status) {
		if (status == null) {
			return null;
		}
		if (Boolean.TRUE.equals(status.getDisetujui()) && status.getWaktuPersetujuan() != null) {
			return status.getWaktuPersetujuan();
		}
		if (Boolean.TRUE.equals(status.getDitolak()) && status.getWaktuDitolak() != null) {
			return status.getWaktuDitolak();
		}
		return status.getTanggal_dirubah();
	}

	private String tooltipDisposisi(AlurPersetujuanSuratMasukStatus status, String label, String statusText,
			String prevLabel, Date prevWaktu) {
		StringBuilder sb = new StringBuilder();
		sb.append(label).append(" - ").append(statusText);
		if (status == null) {
			return sb.toString();
		}
		String pejabat = status.getPejabat() == null ? "" : status.getPejabat().getNama();
		if (pejabat != null && pejabat.trim().length() > 0) {
			sb.append(" - ").append(pejabat);
		}
		Date waktu = null;
		if (Boolean.TRUE.equals(status.getDisetujui())) {
			waktu = status.getWaktuPersetujuan();
		} else if (Boolean.TRUE.equals(status.getDitolak())) {
			waktu = status.getWaktuDitolak();
		}
		if (waktu != null) {
			sb.append(" - ").append(Common.dateFormat3.get().format(waktu));
		}
		if (status.getKeterangan() != null && status.getKeterangan().trim().length() > 0) {
			sb.append(" - ").append(status.getKeterangan().replace('\n', ' '));
		}
		String konseptor = ringkasanKonseptor(status.getKonseptor());
		if (konseptor != null && konseptor.trim().length() > 0) {
			sb.append("\nJabatan yang mendisposisikan : ").append(konseptor);
			Date waktuDisposisi = waktuDisposisi(status);
			if (waktuDisposisi != null) {
				sb.append("\nTanggal & Waktu : ").append(Common.dateFormat3.get().format(waktuDisposisi));
			}
		}
		return sb.toString();
	}

	private String ringkasanKonseptor(Tbmuser konseptor) {
		if (konseptor == null) {
			return "";
		}
		String ringkasan = String.valueOf(konseptor);
		return bersihkanKeteranganKurungKonseptor(ringkasan);
	}

	private String bersihkanKeteranganKurungKonseptor(String ringkasan) {
		if (ringkasan == null || "null".equalsIgnoreCase(ringkasan.trim())) {
			return "";
		}
		return ringkasan.trim().replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim();
	}

	private String labelJabatanKonseptor(Tbmuser konseptor) {
		JenisJabatan jenisJabatan = jenisJabatanKonseptor(konseptor);
		if (jenisJabatan != null && jenisJabatan.getNama() != null && jenisJabatan.getNama().trim().length() > 0) {
			return jenisJabatan.getNama();
		}
		return "";
	}

	private JenisJabatan jenisJabatanKonseptor(Tbmuser konseptor) {
		if (konseptor == null) {
			return null;
		}
		try {
			Tbmrole role = konseptor.hakAkses();
			if (role != null && role.getJenisJabatan() != null) {
				return role.getJenisJabatan();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/dashboard/surat/DasboardSuratMasuk.java:jenisJabatanKonseptor-role");
		}
		try {
			List<Pejabat> pejabats = ConstantValues.simpleList(
					HibernateUtil.currentSession().createCriteria(Pejabat.class)
							.add(Restrictions.or(
									Restrictions.ilike("usernamePengguna", "," + konseptor.getUserId() + ",",
											MatchMode.ANYWHERE),
									Restrictions.or(Restrictions.eq("pegawai", konseptor.getPegawai()),
											Restrictions.or(Restrictions.eq("dosen", konseptor.getDosen()),
													Restrictions.eq("guru", konseptor.getGuru())))))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setMaxResults(1),
					Pejabat.class);
			if (!pejabats.isEmpty() && pejabats.get(0).getJenisJabatan() != null) {
				return pejabats.get(0).getJenisJabatan();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/dashboard/surat/DasboardSuratMasuk.java:jenisJabatanKonseptor-pejabat");
		}
		return null;
	}

	private String namaKonseptor(Tbmuser konseptor) {
		if (konseptor == null) {
			return "";
		}
		if (konseptor.getUserNama() != null && konseptor.getUserNama().trim().length() > 0) {
			return konseptor.getUserNama();
		}
		return konseptor.getUserId() == null ? "" : konseptor.getUserId();
	}

	private void pengajuanBaru(final Tabpanel panelchildren) throws Exception {

		if (mulai == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
			mulai = calendar.getTime();
		}

		EventListener pengajuanBaruEventListener = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(panelchildren);

				Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
				rowUtamapalingAwal.getGrid().setSclass("dgrid");

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(rowUtamapalingAwal);

				final Textbox cari = new Textbox();

				final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
				ambilDataSatuanKerjaBanbox.setCols(8);
				ambilDataSatuanKerjaBanbox.setReadonly(true);
				ambilDataSatuanKerjaBanbox.setParent(toolbar);

				new MyLabelAgakKecil("Cari:").setParent(toolbar);
				cari.setCols(10);
				cari.setParent(toolbar);

				new MyLabelAgakKecil("Tgl:").setParent(toolbar);

				final MyDatebox searchmulai = new MyDatebox(mulai);
				final MyDatebox searchsampai = new MyDatebox(sampai);

				searchmulai.setCols(4);
				searchsampai.setCols(4);

				searchmulai.setReadonly(true);
				searchsampai.setReadonly(true);

				searchmulai.setParent(toolbar);
				searchsampai.setParent(toolbar);

				MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
				refresh.setTooltiptext("Refresh");
				refresh.setParent(toolbar);

				final DataCriteria dataCriteria = new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {

						String c = cari.getValue().trim();

						Session session = HibernateUtil.currentSession();
						Criteria criteria = session.createCriteria(SuratMasuk.class)

								.add(ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja") == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("satuanKerja",
												ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja")))

								.add(searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ge("tanggalSurat", searchmulai.getValue()))
								.add(searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.le("tanggalSurat", searchsampai.getValue()))

								.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.ilike("perihal", c, MatchMode.ANYWHERE),
												Restrictions.or(Restrictions.ilike("noSurat", c, MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("keterangan", c, MatchMode.ANYWHERE),
																Restrictions.or(
																		Restrictions.ilike("kode", c,
																				MatchMode.ANYWHERE),
																		Restrictions.ilike("nama", c,
																				MatchMode.ANYWHERE)))))

								);

						if (order) {
							criteria.addOrder(Order.desc("id"));
						}

						return criteria;
					}
				};

				final Paging paging = new Paging();
				MyFormRow rowUtama = new MyFormRow();
				rowUtama.setParent(rowUtamapalingAwal.getParent());
				rowUtama.appendChild(paging);

				final MyFormRow rowUtamaData = new MyFormRow();
				rowUtamaData.setParent(rowUtamapalingAwal.getParent());

				EventListener dataSearchDefault = new EventListener() {

					@Override
					public void onEvent(Event event) {

						Common.clear(rowUtamaData);

						Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

						List<SuratMasuk> suratMasuks = ((Criteria) dataCriteria.initCriteria(true))
								.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage()))).setMaxResults(5)
								.list();

						MyGrid grid = new MyGrid();
						// grid.setSclass("dgrid");
						grid.setParent(rowUtamaData);
						grid.setSclass("fgrid");
						grid.setStyle("min-height:100px;border:0px;background: transparent;");
						grid.setMold("paging");
						grid.setPageSize(10);
						grid.getPagingChild().setMold("os");

						Rows rows = new Rows();
						rows.setParent(grid);

						for (final SuratMasuk suratMasuk : suratMasuks) {
							MyFormRow rowUtamaLagi = new MyFormRow();
							rowUtamaLagi.setParent(rows);

							Vbox vbox1 = new Vbox();
							vbox1.setParent(rowUtamaLagi);

							Vbox a;
							(a = new Vbox()).setParent(vbox1);
							Vbox vbox = new Vbox();
							a.appendChild(vbox);
							vbox.appendChild(new MyLabelBoldAja(suratMasuk.getNama() + " (tgl "
									+ Common.dateFormat6.get().format(suratMasuk.getTanggalSurat()) + ")"));

							vbox.appendChild(new MyLabelAgakKecil(suratMasuk.getKlasifikasiSuratMasuk() == null ? ""
									: suratMasuk.getKlasifikasiSuratMasuk().getNama()));
							vbox.appendChild(new MyLabelAgakKecil(suratMasuk.getKode()));
							vbox.appendChild(new MyLabelAgakKecil(suratMasuk.getPerihal()));

							if (suratMasuk.getAlurDitolak() != null && suratMasuk.getAlurDitolak().getTelahDirevisi()) {
								try {
									vbox.appendChild(new MyLabelAgakKecilBoldMerah("Direvisi dengan catatan : "
											+ suratMasuk.getAlurDitolak().getCatatanRevisi()));
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/surat/DasboardSuratMasuk.java:252");
								}
								try {
									vbox.appendChild(
											new MyLabelAgakKecilBoldMerah("Sebelumnya ditolak dengan catatan : "
													+ suratMasuk.getAlurDitolak().getKeterangan()));
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/surat/DasboardSuratMasuk.java:259");
								}
							} else if (suratMasuk.getAlurDitolak() != null
									&& suratMasuk.getAlurDitolak().getDitolak()) {
								try {
									vbox.appendChild(new MyLabelAgakKecilBoldMerah(
											"Ditolak dengan catatan : " + suratMasuk.getAlurDitolak().getKeterangan()));
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/surat/DasboardSuratMasuk.java:267");
								}
							}

							String html = "";
							Session session = HibernateUtil.currentSession();
							List<AlurPersetujuanSuratMasukStatus> alurPersetujuanSuratMasukStatuss = session
									.createCriteria(AlurPersetujuanSuratMasukStatus.class)
									.add(Restrictions.isNotNull("kodeUnik"))
									.add(Restrictions.eq("suratMasuk", suratMasuk)).addOrder(Order.asc("id")).list();
							html = buatHtmlDisposisiBergrup(alurPersetujuanSuratMasukStatuss);

							new ais.ui.util.MyHtml(html)
									.setParent(vbox);

							html = "";
							List<String> suratMasukValues = session.createCriteria(OpsiSuratMasukValue.class)
									.setProjection(Projections.groupProperty("nama"))
									.add(Restrictions.eq("suratMasuk", suratMasuk)).list();
							for (String opsiSuratMasukValue : suratMasukValues) {
								html += "<li>" + opsiSuratMasukValue + "</li>";
							}

							new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
									+ Common.getBahasaConfig("Opsi") + ":<ul>" + html + "</ul></font>").setParent(vbox);

							Hbox hbox = new Hbox();
							hbox.setParent(vbox1);

							Session sessions = StreamingHibernateUtil.getInstance().currentSession();
							List<Object[]> fotoGambarSuratMasuks = suratMasuk == null || suratMasuk.getId() == null
									? new ArrayList<Object[]>()
									: sessions.createCriteria(FotoGambarSuratMasuk.class)
											.setProjection(Projections.projectionList().add(Projections.property("id"))
													.add(Projections.property("nama")))
											.add(Restrictions.eq("suratMasuk", suratMasuk.getId()))
											.addOrder(Order.desc("id")).list();

							for (Object[] fotoGambarSuratMasuk : fotoGambarSuratMasuks) {
								try {
									final Long id = (Long) fotoGambarSuratMasuk[0];
									String nama = (String) fotoGambarSuratMasuk[1];

									Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig(nama,
											"/img/svg/download.svg");
									button.setTooltiptext("Download " + nama);
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {

											Session sessions = StreamingHibernateUtil.getInstance().currentSession();

											FotoGambarSuratMasuk fotoGambarSuratMasuk = (FotoGambarSuratMasuk) sessions
													.createCriteria(FotoGambarSuratMasuk.class)
													.add(Restrictions.idEq(id)).uniqueResult();

											if (fotoGambarSuratMasuk.getGdrive() != null
													&& !fotoGambarSuratMasuk.getGdrive().isEmpty()) {
												ExecutionsCtrl.getCurrent().sendRedirect(
														fotoGambarSuratMasuk.downloadGDriveUrl(), "_blank");
											} else if (fotoGambarSuratMasuk != null) {

												Common.display(fotoGambarSuratMasuk);

											}

											sessions.disconnect();
											sessions.close();
											StreamingHibernateUtil.getInstance().closeSession();

										}

									});
									button.setParent(hbox);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/surat/DasboardSuratMasuk.java:466");
									// TODO: handle exception
								}
							}
							sessions.disconnect();
							sessions.close();
							StreamingHibernateUtil.getInstance().closeSession();

						}
						suratMasuks = null;
					}
				};

				Common.initPaging5(paging, dataSearchDefault);
				dataSearchDefault.onEvent(arg0);

				searchsampai.addEventListener("onChange", dataSearchDefault);
				searchmulai.addEventListener("onChange", dataSearchDefault);
				cari.addEventListener("onOK", dataSearchDefault);
				ambilDataSatuanKerjaBanbox.setEventListener(dataSearchDefault);
				refresh.addEventListener("onClick", dataSearchDefault);
			}
		};

		pengajuanBaruEventListener.onEvent(null);
	}

	private void peminjaman(final Tabpanel panelchildren) throws Exception {

		if (mulai == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
			mulai = calendar.getTime();
		}

		EventListener pengajuanBaruEventListener = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(panelchildren);

				Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
				rowUtamapalingAwal.getGrid().setSclass("dgrid");

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(rowUtamapalingAwal);

				final Textbox cari = new Textbox();

				final AmbilDataPeminjamSuratBanbox peminjamSuratBanbox = new AmbilDataPeminjamSuratBanbox();
				peminjamSuratBanbox.setCols(8);
				peminjamSuratBanbox.setReadonly(true);
				peminjamSuratBanbox.setParent(toolbar);

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getPegawai() != null) {
					PeminjamSurat peminjamSurat = (PeminjamSurat) HibernateUtil.currentSession()
							.createCriteria(PeminjamSurat.class).add(Restrictions.eq("pegawai", tbmuser.getPegawai()))
							.setMaxResults(1).uniqueResult();
					if (peminjamSurat != null && peminjamSurat.getId() != null) {
						peminjamSuratBanbox.setAttribute("peminjamSurat", peminjamSurat);
						peminjamSuratBanbox.setName(peminjamSurat.getNama());
						peminjamSuratBanbox.setDisabled(true);
					}
				}

				new MyLabelAgakKecil("Cari:").setParent(toolbar);
				cari.setCols(10);
				cari.setParent(toolbar);

				new MyLabelAgakKecil("Tgl:").setParent(toolbar);

				final MyDatebox searchmulai = new MyDatebox(mulai);
				final MyDatebox searchsampai = new MyDatebox(sampai);

				searchmulai.setCols(4);
				searchsampai.setCols(4);

				searchmulai.setReadonly(true);
				searchsampai.setReadonly(true);

				searchmulai.setParent(toolbar);
				searchsampai.setParent(toolbar);

				MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
				refresh.setTooltiptext("Refresh");
				refresh.setParent(toolbar);

				final DataCriteria dataCriteria = new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {

						String c = cari.getValue().trim();

						Session session = HibernateUtil.currentSession();
						Criteria criteria = session.createCriteria(PeminjamanSuratItemDetail.class)

								.createAlias("peminjamanSuratItem", "peminjamanSuratItem")
								.createAlias("suratMasuk", "suratMasuk")

								.add(peminjamSuratBanbox.getAttribute("peminjamSurat") == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("peminjamanSuratItem.peminjamSurat",
												peminjamSuratBanbox.getAttribute("peminjamSurat")))

								.add(searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.ge("peminjamanSuratItem.mulai", searchmulai.getValue()),
												Restrictions.ge("peminjamanSuratItem.sampai", searchmulai.getValue())))

								.add(searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.le("peminjamanSuratItem.mulai", searchsampai.getValue()),
												Restrictions.le("peminjamanSuratItem.sampai", searchsampai.getValue())))

								.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.ilike("suratMasuk.perihal", c, MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("suratMasuk.noSurat", c, MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("suratMasuk.keterangan", c,
																		MatchMode.ANYWHERE),
																Restrictions.or(
																		Restrictions.ilike("suratMasuk.kode", c,
																				MatchMode.ANYWHERE),
																		Restrictions.ilike("suratMasuk.nama", c,
																				MatchMode.ANYWHERE)))))

								);

						if (order) {
							criteria.addOrder(Order.desc("id"));
						}

						return criteria;
					}
				};

				final Paging paging = new Paging();
				MyFormRow rowUtama = new MyFormRow();
				rowUtama.setParent(rowUtamapalingAwal.getParent());
				rowUtama.appendChild(paging);

				final MyFormRow rowUtamaData = new MyFormRow();
				rowUtamaData.setParent(rowUtamapalingAwal.getParent());

				EventListener dataSearchDefault = new EventListener() {

					@Override
					public void onEvent(Event event) {

						Common.clear(rowUtamaData);

						Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

						List<PeminjamanSuratItemDetail> suratMasuks = ((Criteria) dataCriteria.initCriteria(true))
								.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage()))).setMaxResults(5)
								.list();

						MyGrid grid = new MyGrid();
						// grid.setSclass("dgrid");
						grid.setParent(rowUtamaData);
						grid.setSclass("fgrid");
						grid.setStyle("min-height:100px;border:0px;background: transparent;");
						grid.setMold("paging");
						grid.setPageSize(10);
						grid.getPagingChild().setMold("os");

						Rows rows = new Rows();
						rows.setParent(grid);

						for (final PeminjamanSuratItemDetail peminjamanSuratItemDetail : suratMasuks) {
							SuratMasuk suratMasuk = peminjamanSuratItemDetail.getSuratMasuk();
							MyFormRow rowUtamaLagi = new MyFormRow();
							rowUtamaLagi.setParent(rows);

							Vbox vbox1 = new Vbox();
							vbox1.setParent(rowUtamaLagi);

							Vbox a;
							(a = new Vbox()).setParent(vbox1);
							Vbox vbox = new Vbox();
							a.appendChild(vbox);
							vbox.appendChild(
									new MyLabelBoldAja(suratMasuk.getNama() + " (masa "
											+ Common.dateFormat6.get().format(
													peminjamanSuratItemDetail.getPeminjamanSuratItem().getMulai())
											+ " sd "
											+ Common.dateFormat6.get().format(
													peminjamanSuratItemDetail.getPeminjamanSuratItem().getSampai())
											+ ")"));

							vbox.appendChild(new MyLabelAgakKecil(suratMasuk.getKlasifikasiSuratMasuk() == null ? ""
									: suratMasuk.getKlasifikasiSuratMasuk().getNama()));
							vbox.appendChild(new MyLabelAgakKecil(suratMasuk.getKode()));
							vbox.appendChild(new MyLabelAgakKecil(suratMasuk.getPerihal()));

							Hbox hbox = new Hbox();
							hbox.setParent(vbox1);

							Session sessions = StreamingHibernateUtil.getInstance().currentSession();
							List<Object[]> fotoGambarSuratMasuks = suratMasuk == null || suratMasuk.getId() == null
									? new ArrayList<Object[]>()
									: sessions.createCriteria(FotoGambarSuratMasuk.class)
											.setProjection(Projections.projectionList().add(Projections.property("id"))
													.add(Projections.property("nama")))
											.add(Restrictions.eq("suratMasuk", suratMasuk.getId()))
											.addOrder(Order.desc("id")).list();

							for (Object[] fotoGambarSuratMasuk : fotoGambarSuratMasuks) {
								try {
									final Long id = (Long) fotoGambarSuratMasuk[0];
									String nama = (String) fotoGambarSuratMasuk[1];

									Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig(nama,
											"/img/svg/download.svg");
									button.setTooltiptext("Download " + nama);
									button.setDisabled(peminjamanSuratItemDetail.getPeminjamanSuratItem()
											.getDisetujuiOleh() == null
											|| peminjamanSuratItemDetail.getPeminjamanSuratItem().getMulai()
													.after(WaktuUtil.getDate())
											|| peminjamanSuratItemDetail.getPeminjamanSuratItem().getSampai()
													.before(WaktuUtil.getDate()));
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {

											Session sessions = StreamingHibernateUtil.getInstance().currentSession();

											FotoGambarSuratMasuk fotoGambarSuratMasuk = (FotoGambarSuratMasuk) sessions
													.createCriteria(FotoGambarSuratMasuk.class)
													.add(Restrictions.idEq(id)).uniqueResult();

											if (fotoGambarSuratMasuk.getGdrive() != null
													&& !fotoGambarSuratMasuk.getGdrive().isEmpty()) {
												ExecutionsCtrl.getCurrent().sendRedirect(
														fotoGambarSuratMasuk.downloadGDriveUrl(), "_blank");
											} else if (fotoGambarSuratMasuk != null) {

												Common.display(fotoGambarSuratMasuk);

											}

											sessions.disconnect();
											sessions.close();
											StreamingHibernateUtil.getInstance().closeSession();

										}

									});
									button.setParent(hbox);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/surat/DasboardSuratMasuk.java:721");
									// TODO: handle exception
								}
							}
							sessions.disconnect();
							sessions.close();
							StreamingHibernateUtil.getInstance().closeSession();

						}
						suratMasuks = null;
					}
				};

				Common.initPaging5(paging, dataSearchDefault);
				dataSearchDefault.onEvent(arg0);

				searchsampai.addEventListener("onChange", dataSearchDefault);
				searchmulai.addEventListener("onChange", dataSearchDefault);
				cari.addEventListener("onOK", dataSearchDefault);
				peminjamSuratBanbox.setEventListener(dataSearchDefault);
				refresh.addEventListener("onClick", dataSearchDefault);
			}
		};

		pengajuanBaruEventListener.onEvent(null);
	}

	private void kembali(final Tabpanel panelchildren) throws Exception {

		if (mulai == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
			mulai = calendar.getTime();
		}

		EventListener pengajuanBaruEventListener = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(panelchildren);

				Row rowUtamapalingAwal = Common.tampilanScroll1(panelchildren);
				rowUtamapalingAwal.getGrid().setSclass("dgrid");

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(rowUtamapalingAwal);

				final Textbox cari = new Textbox();

				final AmbilDataPeminjamSuratBanbox peminjamSuratBanbox = new AmbilDataPeminjamSuratBanbox();
				peminjamSuratBanbox.setCols(8);
				peminjamSuratBanbox.setReadonly(true);
				peminjamSuratBanbox.setParent(toolbar);

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getPegawai() != null) {
					PeminjamSurat peminjamSurat = (PeminjamSurat) HibernateUtil.currentSession()
							.createCriteria(PeminjamSurat.class).add(Restrictions.eq("pegawai", tbmuser.getPegawai()))
							.setMaxResults(1).uniqueResult();
					if (peminjamSurat != null && peminjamSurat.getId() != null) {
						peminjamSuratBanbox.setAttribute("peminjamSurat", peminjamSurat);
						peminjamSuratBanbox.setName(peminjamSurat.getNama());
						peminjamSuratBanbox.setDisabled(true);
					}
				}

				new MyLabelAgakKecil("Cari:").setParent(toolbar);
				cari.setCols(10);
				cari.setParent(toolbar);

				new MyLabelAgakKecil("Tgl:").setParent(toolbar);

				final MyDatebox searchmulai = new MyDatebox(mulai);
				final MyDatebox searchsampai = new MyDatebox(sampai);

				searchmulai.setCols(4);
				searchsampai.setCols(4);

				searchmulai.setReadonly(true);
				searchsampai.setReadonly(true);

				searchmulai.setParent(toolbar);
				searchsampai.setParent(toolbar);

				MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
				refresh.setTooltiptext("Refresh");
				refresh.setParent(toolbar);

				final DataCriteria dataCriteria = new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {

						String c = cari.getValue().trim();

						Session session = HibernateUtil.currentSession();
						Criteria criteria = session.createCriteria(KembaliSuratItemDetail.class)

								.createAlias("kembaliSuratItem", "kembaliSuratItem")
								.createAlias("kembaliSuratItem.peminjamanSuratItem", "peminjamanSuratItem")

								.createAlias("suratMasuk", "suratMasuk")

								.add(peminjamSuratBanbox.getAttribute("peminjamSurat") == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("peminjamanSuratItem.peminjamSurat",
												peminjamSuratBanbox.getAttribute("peminjamSurat")))

								.add(searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.ge("peminjamanSuratItem.mulai", searchmulai.getValue()),
												Restrictions.ge("peminjamanSuratItem.sampai", searchmulai.getValue())))

								.add(searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.le("peminjamanSuratItem.mulai", searchsampai.getValue()),
												Restrictions.le("peminjamanSuratItem.sampai", searchsampai.getValue())))

								.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.ilike("suratMasuk.perihal", c, MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("suratMasuk.noSurat", c, MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("suratMasuk.keterangan", c,
																		MatchMode.ANYWHERE),
																Restrictions.or(
																		Restrictions.ilike("suratMasuk.kode", c,
																				MatchMode.ANYWHERE),
																		Restrictions.ilike("suratMasuk.nama", c,
																				MatchMode.ANYWHERE)))))

								);

						if (order) {
							criteria.addOrder(Order.desc("id"));
						}

						return criteria;
					}
				};

				final Paging paging = new Paging();
				MyFormRow rowUtama = new MyFormRow();
				rowUtama.setParent(rowUtamapalingAwal.getParent());
				rowUtama.appendChild(paging);

				final MyFormRow rowUtamaData = new MyFormRow();
				rowUtamaData.setParent(rowUtamapalingAwal.getParent());

				EventListener dataSearchDefault = new EventListener() {

					@Override
					public void onEvent(Event event) {

						Common.clear(rowUtamaData);

						Common.initPaging5((Criteria) dataCriteria.initCriteria(false), paging);

						List<KembaliSuratItemDetail> suratMasuks = ((Criteria) dataCriteria.initCriteria(true))
								.setFirstResult(5 * ((paging == null ? 0 : paging.getActivePage()))).setMaxResults(5)
								.list();

						MyGrid grid = new MyGrid();
						// grid.setSclass("dgrid");
						grid.setParent(rowUtamaData);
						grid.setSclass("fgrid");
						grid.setStyle("min-height:100px;border:0px;background: transparent;");
						grid.setMold("paging");
						grid.setPageSize(10);
						grid.getPagingChild().setMold("os");

						Rows rows = new Rows();
						rows.setParent(grid);

						for (final KembaliSuratItemDetail kembaliSuratItemDetail : suratMasuks) {
							SuratMasuk suratMasuk = kembaliSuratItemDetail.getSuratMasuk();
							MyFormRow rowUtamaLagi = new MyFormRow();
							rowUtamaLagi.setParent(rows);

							Vbox vbox1 = new Vbox();
							vbox1.setParent(rowUtamaLagi);

							Vbox a;
							(a = new Vbox()).setParent(vbox1);
							Vbox vbox = new Vbox();
							a.appendChild(vbox);
							vbox.appendChild(new MyLabelBoldAja(suratMasuk.getNama() + " (masa "
									+ Common.dateFormat6.get().format(kembaliSuratItemDetail.getPeminjamanSuratItemDetail()
											.getPeminjamanSuratItem().getMulai())
									+ " sd " + Common.dateFormat6.get().format(kembaliSuratItemDetail
											.getPeminjamanSuratItemDetail().getPeminjamanSuratItem().getSampai())
									+ ")"));

							vbox.appendChild(new MyLabelAgakKecil(suratMasuk.getKlasifikasiSuratMasuk() == null ? ""
									: suratMasuk.getKlasifikasiSuratMasuk().getNama()));
							vbox.appendChild(new MyLabelAgakKecil(suratMasuk.getKode()));
							vbox.appendChild(new MyLabelAgakKecil(suratMasuk.getPerihal()));

							Hbox hbox = new Hbox();
							hbox.setParent(vbox1);

							Session sessions = StreamingHibernateUtil.getInstance().currentSession();
							List<Object[]> fotoGambarSuratMasuks = suratMasuk == null || suratMasuk.getId() == null
									? new ArrayList<Object[]>()
									: sessions.createCriteria(FotoGambarSuratMasuk.class)
											.setProjection(Projections.projectionList().add(Projections.property("id"))
													.add(Projections.property("nama")))
											.add(Restrictions.eq("suratMasuk", suratMasuk.getId()))
											.addOrder(Order.desc("id")).list();

							for (Object[] fotoGambarSuratMasuk : fotoGambarSuratMasuks) {
								try {
									final Long id = (Long) fotoGambarSuratMasuk[0];
									String nama = (String) fotoGambarSuratMasuk[1];

									Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig(nama,
											"/img/svg/download.svg");
									button.setTooltiptext("Download " + nama);
									button.setDisabled(kembaliSuratItemDetail.getPeminjamanSuratItemDetail()
											.getPeminjamanSuratItem().getDisetujuiOleh() == null
											|| kembaliSuratItemDetail.getPeminjamanSuratItemDetail()
													.getPeminjamanSuratItem().getMulai().after(WaktuUtil.getDate())
											|| kembaliSuratItemDetail.getPeminjamanSuratItemDetail()
													.getPeminjamanSuratItem().getSampai().before(WaktuUtil.getDate()));
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {

											Session sessions = StreamingHibernateUtil.getInstance().currentSession();

											FotoGambarSuratMasuk fotoGambarSuratMasuk = (FotoGambarSuratMasuk) sessions
													.createCriteria(FotoGambarSuratMasuk.class)
													.add(Restrictions.idEq(id)).uniqueResult();

											if (fotoGambarSuratMasuk.getGdrive() != null
													&& !fotoGambarSuratMasuk.getGdrive().isEmpty()) {
												ExecutionsCtrl.getCurrent().sendRedirect(
														fotoGambarSuratMasuk.downloadGDriveUrl(), "_blank");
											} else if (fotoGambarSuratMasuk != null) {

												Common.display(fotoGambarSuratMasuk);

											}

											sessions.disconnect();
											sessions.close();
											StreamingHibernateUtil.getInstance().closeSession();

										}

									});
									button.setParent(hbox);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/surat/DasboardSuratMasuk.java:976");
									// TODO: handle exception
								}
							}
							sessions.disconnect();
							sessions.close();
							StreamingHibernateUtil.getInstance().closeSession();

						}
						suratMasuks = null;
					}
				};

				Common.initPaging5(paging, dataSearchDefault);
				dataSearchDefault.onEvent(arg0);

				searchsampai.addEventListener("onChange", dataSearchDefault);
				searchmulai.addEventListener("onChange", dataSearchDefault);
				cari.addEventListener("onOK", dataSearchDefault);
				peminjamSuratBanbox.setEventListener(dataSearchDefault);
				refresh.addEventListener("onClick", dataSearchDefault);
			}
		};

		pengajuanBaruEventListener.onEvent(null);
	}

	public DasboardSuratMasuk(String tipe) {
		super();
		this.tipe = tipe;
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardSuratMasuk(String tipe, String title, String border, boolean closable) {
		super(title, border, closable);
		this.tipe = tipe;
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Tabbox tabbox = new Tabbox();
		tabbox.setStyle("min-height:" + desktopHeight + "px");
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (desktopHeight != null) {
				tabbox.setStyle("min-height:" + (desktopHeight * 0.9) + "px");
			}
		}

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Klasifikasi");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Sifat");
		tab2.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Masa Berlaku");
		tab3.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("Loker");
		tab4.setParent(tabs);

		MyTabConfig tab5 = new MyTabConfig("Status Dipertahankan");
		tab5.setParent(tabs);

		MyTabConfig tab6 = new MyTabConfig("Pencarian Surat Masuk");
		tab6.setVisible(Common.getApakahAdmin());
		tab6.setParent(tabs);

		MyTabConfig tab7 = new MyTabConfig("Peminjaman");
		tab7.setParent(tabs);

		MyTabConfig tab8 = new MyTabConfig("Pengembalian");
		tab8.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight(desktopHeight + "px");

		final Tabpanel panelSifat = new ais.ui.util.MyTabpanel();
		panelSifat.setParent(tabpanels);
		panelSifat.setHeight(desktopHeight + "px");

		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelSifat.getChildren().isEmpty()) {
					DasboardSuratMasukSifatSurat dashboardSifat = new DasboardSuratMasukSifatSurat(tipe);
					dashboardSifat.setHeight(desktopHeight + "px");
					dashboardSifat.setWidth("100%");

					panelSifat.appendChild(dashboardSifat);
				}
			}
		});

		final Tabpanel panelMasaBerlaku = new ais.ui.util.MyTabpanel();
		panelMasaBerlaku.setParent(tabpanels);
		panelMasaBerlaku.setHeight(desktopHeight + "px");

		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelMasaBerlaku.getChildren().isEmpty()) {
					DasboardSuratMasukMasaBerlaku dashboard = new DasboardSuratMasukMasaBerlaku(tipe);
					dashboard.setHeight(desktopHeight + "px");
					dashboard.setWidth("100%");

					panelMasaBerlaku.appendChild(dashboard);
				}
			}
		});

		final Tabpanel panelLoker = new ais.ui.util.MyTabpanel();
		panelLoker.setParent(tabpanels);
		panelLoker.setHeight(desktopHeight + "px");

		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelLoker.getChildren().isEmpty()) {
					DasboardSuratMasukLokerSurat dashboard = new DasboardSuratMasukLokerSurat(tipe);
					dashboard.setHeight(desktopHeight + "px");
					dashboard.setWidth("100%");

					panelLoker.appendChild(dashboard);
				}
			}
		});

		final Tabpanel panelStatusDipertahankan = new ais.ui.util.MyTabpanel();
		panelStatusDipertahankan.setParent(tabpanels);
		panelStatusDipertahankan.setHeight(desktopHeight + "px");

		tab5.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelStatusDipertahankan.getChildren().isEmpty()) {
					DasboardSuratMasukStatusDipertahankan dashboard = new DasboardSuratMasukStatusDipertahankan(tipe);
					dashboard.setHeight(desktopHeight + "px");
					dashboard.setWidth("100%");

					panelStatusDipertahankan.appendChild(dashboard);
				}
			}
		});

		final Tabpanel panelPencarianSuratMasuk = new ais.ui.util.MyTabpanel();
		panelPencarianSuratMasuk.setParent(tabpanels);
		panelPencarianSuratMasuk.setHeight(desktopHeight + "px");
		panelPencarianSuratMasuk.setVisible(Common.getApakahAdmin());
		tab6.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelPencarianSuratMasuk.getChildren().isEmpty()) {
					pengajuanBaru(panelPencarianSuratMasuk);
				}
			}
		});

		final Tabpanel panelPeminjaman = new ais.ui.util.MyTabpanel();
		panelPeminjaman.setParent(tabpanels);
		panelPeminjaman.setHeight(desktopHeight + "px");
		tab7.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelPeminjaman.getChildren().isEmpty()) {
					peminjaman(panelPeminjaman);
				}
			}
		});

		final Tabpanel panelKembali = new ais.ui.util.MyTabpanel();
		panelKembali.setParent(tabpanels);
		panelKembali.setHeight(desktopHeight + "px");
		tab8.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelKembali.getChildren().isEmpty()) {
					kembali(panelKembali);
				}
			}
		});

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(tabpanel1);
		borderlayout.setStyle("min-height:" + (desktopHeight * 0.9) + "px");

		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}

		};

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		searchparent = new AmbilDataSatuanKerjaBanbox();

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		row.appendChild(new MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchparent);
		searchparent.setWidth("95%");

		row.appendChild(new MyLabelConfig("Loker Surat"));
		loker = new Combobox();
		row.appendChild(loker);
		loker.setReadonly(true);
		loker.addEventListener("onChange", eventListener);
		loker.setWidth("95%");
		final EventListener eventListenerD = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
				Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
				if (parent != null) {
					satuanKerjas.clear();
					satuanKerjas.add(parent);
					satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
				}

				Common.insertComboDanSemua(loker, new String[] { "kode", "nama" }, "keterangan", LokerSurat.class,

						Restrictions.and(
								Restrictions.or(Restrictions.isNull("satuanKerja"),
										satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
												: Restrictions.or(Restrictions.isNull("satuanKerja"),
														Restrictions.in("satuanKerja", satuanKerjas))),

								Restrictions.and(Restrictions.eq("tipe", tipe), Restrictions
										.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

			}
		};

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListenerD.onEvent(arg0);
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						reload();
					}
				});
			}
		});
		Common.createDefaultTimerNoBusy(eventListenerD);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal")));
		row.appendChild(start = new MyDatebox());
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("sd")));
		row.appendChild(end = new MyDatebox());

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 12);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		start.addEventListener("onChange", eventListener);
		end.addEventListener("onChange", eventListener);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		Common.createDefaultTimer(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "8");
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DasboardSuratMasuk.this.grid);
			}
		});
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void reload() {
		Common.clear(center);
		grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		ais.ui.util.ZkCompat.setFixedLayout(grid, true);

		List<SatuanKerja> satuanKerjas;

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		if (parent != null) {
			Set<SatuanKerja> temp = new HashSet<SatuanKerja>();
			if (parent != null) {
				temp.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, temp);
			}
			satuanKerjas = new ArrayList<SatuanKerja>(temp);
			Collections.sort(satuanKerjas);
		} else {
			satuanKerjas = new ArrayList<SatuanKerja>(ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas());
			Collections.sort(satuanKerjas);
		}

		List<KlasifikasiSuratMasuk> klasifikasiSuratMasuks = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(SuratMasuk.class)
						.add(loker.getSelectedItem() == null || loker.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("loker", loker.getSelectedItem().getValue()))
						.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
								satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("satuanKerja"),
												Restrictions.in("satuanKerja", satuanKerjas))))

						.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))
						.setProjection(Projections.groupProperty("klasifikasiSuratMasuk.id"))
						.add(Restrictions.isNotNull("klasifikasiSuratMasuk")).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				KlasifikasiSuratMasuk.class, false);

		Columns contents = new Columns();
		contents.setParent(grid);

		MyColumnConfig columnUtama = new MyColumnConfig("Satuan Kerja");
		columnUtama.setParent(contents);

		klasifikasiSuratMasuks.add(null);
		Map<Long, MyColumnConfig> listCols = new HashMap<Long, MyColumnConfig>();
		for (KlasifikasiSuratMasuk klasifikasiSuratMasuk : klasifikasiSuratMasuks) {
			MyColumnConfig column = new MyColumnConfig(
					klasifikasiSuratMasuk == null ? "Tidak ditentukan" : klasifikasiSuratMasuk.getNama());
			column.setTooltiptext(klasifikasiSuratMasuk == null ? "Tidak ditentukan" : klasifikasiSuratMasuk.getNama());
			column.setWidth("5%");
			column.setAlign("right");
			listCols.put(klasifikasiSuratMasuk == null || klasifikasiSuratMasuk.getId() == null ? -1L : klasifikasiSuratMasuk.getId(), column);
		}

		String inSatker = "";
		for (SatuanKerja satuanKerja : satuanKerjas) {
			inSatker += inSatker.isEmpty() ? satuanKerja.getId().toString() : "," + satuanKerja.getId();
		}
		final String satker = inSatker.isEmpty() ? "true"
				: "(this_.satuan_kerja in (" + inSatker + ") or this_.satuan_kerja is null)";

		satuanKerjas.add(null);

		Rows rows = new Rows();
		rows.setParent(grid);

		HtmlCategoryModel categoryModel = new HtmlCategoryModel();
		categoryModel.clear();
		Map<Long, Integer> listTotals = new HashMap<Long, Integer>();
		Map<Long, List<Integer>> mapData = new HashMap<Long, List<Integer>>();
		for (final SatuanKerja satuanKerja : satuanKerjas) {
			List<Integer> data = mapData.get(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());
			if (data == null) {
				data = new ArrayList<Integer>();
				mapData.put(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId(), data);
			}
			for (KlasifikasiSuratMasuk klasifikasiSuratMasuk : klasifikasiSuratMasuks) {
				Integer count = ((Number) HibernateUtil.currentSession().createCriteria(SuratMasuk.class)
						.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

						.add(Restrictions.sqlRestriction("date(this_.tanggalsurat) between date('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + "')"))

						.add(Restrictions.sqlRestriction(satker))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.rowCount())
						.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
								: Restrictions.eq("satuanKerja", satuanKerja))
						.add(klasifikasiSuratMasuk == null ? Restrictions.isNull("klasifikasiSuratMasuk")
								: Restrictions.eq("klasifikasiSuratMasuk", klasifikasiSuratMasuk))
						.add(loker.getSelectedItem() == null || loker.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("loker", loker.getSelectedItem().getValue()))
						.uniqueResult()).intValue();
				data.add(count);
				Integer colCount = listTotals.get(klasifikasiSuratMasuk == null || klasifikasiSuratMasuk.getId() == null ? -1L : klasifikasiSuratMasuk.getId());
				if (colCount == null) {
					colCount = 0;
				}
				colCount += count;
				listTotals.put(klasifikasiSuratMasuk == null || klasifikasiSuratMasuk.getId() == null ? -1L : klasifikasiSuratMasuk.getId(), colCount);

			}
		}

		Set<String> countData = new HashSet<String>();
		for (final SatuanKerja satuanKerja : satuanKerjas) {
			List<Integer> data = mapData.get(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.appendChild(new MyLabelBoldAja(satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama()));
			int jml = 0;
			int i = 0;
			for (final KlasifikasiSuratMasuk klasifikasiSuratMasuk : klasifikasiSuratMasuks) {
				Integer colCount = listTotals.get(klasifikasiSuratMasuk == null || klasifikasiSuratMasuk.getId() == null ? -1L : klasifikasiSuratMasuk.getId());
				if (colCount == null) {
					colCount = 0;
				}
				if (colCount > 0) {

					countData.add((klasifikasiSuratMasuk == null ? "" : "E" + klasifikasiSuratMasuk.getId()));

					int count = data.get(i);
					jml += count;

					if (count > 0) {
						countData.add((satuanKerja == null ? "" : "S" + satuanKerja.getId()));
						categoryModel.setValue(satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama(),
								klasifikasiSuratMasuk == null ? "Tidak ditentukan" : klasifikasiSuratMasuk.getNama(),
								count);
					}
					A a = new A(count + "");
					a.setStyle("font-size:12px;");
					a.setParent(row);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							EventListener eventListener = (EventListener) Common
									.cetakDataCustomButton(SuratMasuk.class, new DataCriteriaWithColumn() {

										@Override
										public Object[] initCriteria(boolean order) {

											try {

												Criteria criteria = HibernateUtil.currentSession()
														.createCriteria(SuratMasuk.class)
														.add(Restrictions.or(Restrictions.isNull("tipe"),
																Restrictions.eq("tipe", tipe)))

														.add(Restrictions.sqlRestriction(
																"date(this_.tanggalsurat) between date('"
																		+ Common.databaseDateFormat.get()
																				.format(start.getValue())
																		+ "') and date('"
																		+ Common.databaseDateFormat.get()
																				.format(end.getValue())
																		+ "')"))

														.add(Restrictions.sqlRestriction(satker))
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
																: Restrictions.eq("satuanKerja", satuanKerja))
														.add(klasifikasiSuratMasuk == null
																? Restrictions.isNull("klasifikasiSuratMasuk")
																: Restrictions.eq("klasifikasiSuratMasuk",
																		klasifikasiSuratMasuk))
														.add(loker.getSelectedItem() == null
																|| loker.getSelectedItem().getValue() == null
																		? Restrictions.sqlRestriction("true")
																		: Restrictions.eq("loker",
																				loker.getSelectedItem().getValue()));

												return new Object[] { criteria, SuratMasukAction.contents };

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
											return null;
										}

									}, null, "Download Data", "/img/print.png", null, null, false, null,
											"DATA TAMBAHAN",
											new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"" })
									.getAttribute("eventListener");

							eventListener.onEvent(null);
						}
					});
				}
				i++;
			}

			A a = new A(jml + "");
			a.setStyle("font-size:12px;");
			a.setParent(row);
			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					EventListener eventListener = (EventListener) Common
							.cetakDataCustomButton(SuratMasuk.class, new DataCriteriaWithColumn() {

								@Override
								public Object[] initCriteria(boolean order) {

									try {

										Criteria criteria = HibernateUtil.currentSession()
												.createCriteria(SuratMasuk.class)
												.add(Restrictions.or(Restrictions.isNull("tipe"),
														Restrictions.eq("tipe", tipe)))

												.add(Restrictions
														.sqlRestriction("date(this_.tanggalsurat) between date('"
																+ Common.databaseDateFormat.get().format(start.getValue())
																+ "') and date('"
																+ Common.databaseDateFormat.get().format(end.getValue())
																+ "')"))

												.add(Restrictions.sqlRestriction(satker))
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
														: Restrictions.eq("satuanKerja", satuanKerja))
												.add(loker.getSelectedItem() == null
														|| loker.getSelectedItem().getValue() == null
																? Restrictions.sqlRestriction("true")
																: Restrictions.eq("loker",
																		loker.getSelectedItem().getValue()));

										return new Object[] { criteria, SuratMasukAction.contents };

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
									return null;
								}

							}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
									new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" })
							.getAttribute("eventListener");

					eventListener.onEvent(null);
				}
			});

			if (jml > 0) {
				row.setParent(rows);
			}
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelBolder("Total"));
		int countCol = 0;
		int totalCol = 0;
		for (final KlasifikasiSuratMasuk klasifikasiSuratMasuk : klasifikasiSuratMasuks) {
			Integer colCount = listTotals.get(klasifikasiSuratMasuk == null || klasifikasiSuratMasuk.getId() == null ? -1L : klasifikasiSuratMasuk.getId());
			if (colCount == null) {
				colCount = 0;
			}
			totalCol += colCount;
			if (colCount > 0) {
				countCol++;
				listCols.get(klasifikasiSuratMasuk == null || klasifikasiSuratMasuk.getId() == null ? -1L : klasifikasiSuratMasuk.getId()).setParent(contents);

				A a = new A(colCount + "");
				a.setStyle("font-size:16px;font-weight: bolder;");
				a.setParent(row);
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						EventListener eventListener = (EventListener) Common
								.cetakDataCustomButton(SuratMasuk.class, new DataCriteriaWithColumn() {

									@Override
									public Object[] initCriteria(boolean order) {

										try {

											Criteria criteria = HibernateUtil.currentSession()
													.createCriteria(SuratMasuk.class)
													.add(Restrictions.or(Restrictions.isNull("tipe"),
															Restrictions.eq("tipe", tipe)))

													.add(Restrictions
															.sqlRestriction("date(this_.tanggalsurat) between date('"
																	+ Common.databaseDateFormat.get().format(start.getValue())
																	+ "') and date('"
																	+ Common.databaseDateFormat.get().format(end.getValue())
																	+ "')"))

													.add(Restrictions.sqlRestriction(satker))
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(klasifikasiSuratMasuk == null
															? Restrictions.isNull("klasifikasiSuratMasuk")
															: Restrictions.eq("klasifikasiSuratMasuk",
																	klasifikasiSuratMasuk))
													.add(loker.getSelectedItem() == null
															|| loker.getSelectedItem().getValue() == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("loker",
																			loker.getSelectedItem().getValue()));

											return new Object[] { criteria, SuratMasukAction.contents };

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
										return null;
									}

								}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
										new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "" })
								.getAttribute("eventListener");

						eventListener.onEvent(null);
					}
				});
			}

		}

		if (countCol > 15) {
			columnUtama.setWidth("15%");
		}

		A a = new A(totalCol + "");
		a.setStyle("font-size:16px;font-weight: bolder;");
		a.setParent(row);
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				EventListener eventListener = (EventListener) Common
						.cetakDataCustomButton(SuratMasuk.class, new DataCriteriaWithColumn() {

							@Override
							public Object[] initCriteria(boolean order) {

								try {

									Criteria criteria = HibernateUtil.currentSession().createCriteria(SuratMasuk.class)
											.add(Restrictions.or(Restrictions.isNull("tipe"),
													Restrictions.eq("tipe", tipe)))

											.add(Restrictions.sqlRestriction("date(this_.tanggalsurat) between date('"
													+ Common.databaseDateFormat.get().format(start.getValue())
													+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue())
													+ "')"))

											.add(Restrictions.sqlRestriction(satker))
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(loker.getSelectedItem() == null
													|| loker.getSelectedItem().getValue() == null
															? Restrictions.sqlRestriction("true")
															: Restrictions.eq("loker",
																	loker.getSelectedItem().getValue()));

									return new Object[] { criteria, SuratMasukAction.contents };

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
								return null;
							}

						}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
								new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "" })
						.getAttribute("eventListener");

				eventListener.onEvent(null);
			}
		});

		MyColumnConfig column = new MyColumnConfig("Total");
		column.setWidth("5%");
		column.setTooltiptext("Total");
		column.setAlign("right");
		column.setParent(contents);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelBolder("Persen"));
		for (KlasifikasiSuratMasuk klasifikasiSuratMasuk : klasifikasiSuratMasuks) {
			Integer colCount = listTotals.get(klasifikasiSuratMasuk == null || klasifikasiSuratMasuk.getId() == null ? -1L : klasifikasiSuratMasuk.getId());
			if (colCount == null) {
				colCount = 0;
			}
			if (colCount > 0) {
				new MyLabelBolder(Common.numberFormat.get().format((colCount.doubleValue() * 100.0) / totalCol) + "%")
						.setParent(row);
			}
		}
		new MyLabelBolder("100%").setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.setSpans((2 + countCol) + "");
		row.setAlign("center");
		Html mychart = new Html(buildModernDashboardSuratChartHtml("Ringkasan Grafik Surat",
				"Grafik ini memakai HTML/CSS modern yang ringan. Data tetap menampilkan perbandingan jumlah surat berdasarkan satuan kerja dan kategori yang dipilih.",
				categoryModel));
		row.appendChild(mychart);
		mychart.setStyle("display:block;width:100%;");

	}

	private static class HtmlCategoryModel {
		private Map<String, Map<String, Number>> values = new LinkedHashMap<String, Map<String, Number>>();

		public void clear() {
			values.clear();
		}

		public void setValue(String series, String category, Number value) {
			if (series == null || series.trim().length() == 0) {
				series = "Tidak Ditentukan";
			}
			if (category == null || category.trim().length() == 0) {
				category = "Tidak Ditentukan";
			}
			Map<String, Number> row = values.get(series);
			if (row == null) {
				row = new LinkedHashMap<String, Number>();
				values.put(series, row);
			}
			row.put(category, value == null ? Integer.valueOf(0) : value);
		}

		public Map<String, Map<String, Number>> getValues() {
			return values;
		}

		public int getTotal() {
			int total = 0;
			for (Map<String, Number> row : values.values()) {
				for (Number number : row.values()) {
					total += number == null ? 0 : number.intValue();
				}
			}
			return total;
		}

		public int getMaxRowTotal() {
			int max = 0;
			for (Map<String, Number> row : values.values()) {
				int total = 0;
				for (Number number : row.values()) {
					total += number == null ? 0 : number.intValue();
				}
				if (total > max) {
					max = total;
				}
			}
			return max <= 0 ? 1 : max;
		}
	}

	private String buildModernDashboardSuratChartHtml(String title, String description, HtmlCategoryModel model) {
		StringBuilder sb = new StringBuilder();
		if (model == null || model.getValues().isEmpty()) {
			sb.append("<div style='padding:18px;border-radius:18px;background:#fff;border:1px solid #e5e7eb;")
					.append("box-shadow:0 12px 26px rgba(15,23,42,.08);font-family:Arial,sans-serif;color:#334155;'>")
					.append("<div style='font-size:16px;font-weight:900;color:#0f172a;'>").append(safeHtml(title))
					.append("</div>")
					.append("<div style='font-size:12px;line-height:1.55;color:#64748b;margin-top:6px;'>")
					.append(safeHtml(description)).append("</div>")
					.append("<div style='margin-top:14px;padding:12px;border-radius:14px;background:#f8fafc;border:1px dashed #cbd5e1;'>")
					.append("Belum ada data yang cocok dengan filter saat ini.</div></div>");
			return sb.toString();
		}

		int total = model.getTotal();
		int maxRow = model.getMaxRowTotal();
		String[] colors = new String[] { "#2563eb", "#16a34a", "#f59e0b", "#dc2626", "#7c3aed", "#0891b2",
				"#db2777", "#4f46e5", "#65a30d", "#9333ea" };

		sb.append("<div style='padding:18px;border-radius:18px;background:#fff;border:1px solid #e5e7eb;")
				.append("box-shadow:0 12px 26px rgba(15,23,42,.08);font-family:Arial,sans-serif;color:#334155;'>");
		sb.append("<div style='display:flex;align-items:flex-start;justify-content:space-between;gap:12px;flex-wrap:wrap;'>")
				.append("<div style='min-width:240px;flex:1;'>")
				.append("<div style='font-size:11px;letter-spacing:.12em;text-transform:uppercase;color:#0f766e;font-weight:900;'>")
				.append("Grafik HTML/CSS</div>")
				.append("<div style='font-size:18px;font-weight:900;color:#0f172a;margin-top:4px;'>")
				.append(safeHtml(title)).append("</div>")
				.append("<div style='font-size:12px;line-height:1.55;color:#64748b;margin-top:7px;'>")
				.append(safeHtml(description)).append("</div>")
				.append("</div>")
				.append("<div style='padding:10px 14px;border-radius:16px;background:#ecfdf5;border:1px solid #bbf7d0;text-align:right;'>")
				.append("<div style='font-size:11px;color:#166534;font-weight:800;'>Total Dokumen</div>")
				.append("<div style='font-size:24px;color:#14532d;font-weight:900;'>").append(formatNumber(total))
				.append("</div></div></div>");

		sb.append("<div style='margin-top:16px;display:flex;gap:8px;flex-wrap:wrap;'>");
		int categoryIndex = 0;
		Map<String, String> categoryColors = new LinkedHashMap<String, String>();
		for (Map<String, Number> row : model.getValues().values()) {
			for (String category : row.keySet()) {
				if (!categoryColors.containsKey(category)) {
					categoryColors.put(category, colors[categoryIndex % colors.length]);
					categoryIndex++;
				}
			}
		}
		for (Map.Entry<String, String> entry : categoryColors.entrySet()) {
			sb.append("<span style='display:inline-flex;align-items:center;gap:6px;font-size:11px;color:#334155;")
					.append("background:#f8fafc;border:1px solid #e2e8f0;border-radius:999px;padding:5px 9px;'>")
					.append("<i style='display:inline-block;width:10px;height:10px;border-radius:999px;background:")
					.append(entry.getValue()).append(";'></i>")
					.append(safeHtml(entry.getKey())).append("</span>");
		}
		sb.append("</div>");

		sb.append("<div style='margin-top:16px;display:flex;flex-direction:column;gap:12px;'>");
		for (Map.Entry<String, Map<String, Number>> rowEntry : model.getValues().entrySet()) {
			int rowTotal = 0;
			for (Number number : rowEntry.getValue().values()) {
				rowTotal += number == null ? 0 : number.intValue();
			}
			double rowPercent = maxRow <= 0 ? 0.0 : (rowTotal * 100.0 / maxRow);
			if (rowPercent < 3.0 && rowTotal > 0) {
				rowPercent = 3.0;
			}
			sb.append("<div style='padding:12px;border:1px solid #e5e7eb;border-radius:15px;background:#f8fafc;'>")
					.append("<div style='display:flex;align-items:center;justify-content:space-between;gap:10px;'>")
					.append("<div style='font-size:12px;font-weight:900;color:#0f172a;'>")
					.append(safeHtml(rowEntry.getKey())).append("</div>")
					.append("<div style='font-size:12px;font-weight:900;color:#0f766e;'>")
					.append(formatNumber(rowTotal)).append("</div></div>")
					.append("<div style='height:16px;margin-top:8px;background:#e2e8f0;border-radius:999px;overflow:hidden;display:flex;'>");

			for (Map.Entry<String, Number> valueEntry : rowEntry.getValue().entrySet()) {
				int value = valueEntry.getValue() == null ? 0 : valueEntry.getValue().intValue();
				if (value <= 0 || rowTotal <= 0) {
					continue;
				}
				double width = value * 100.0 / rowTotal;
				if (width < 2.0) {
					width = 2.0;
				}
				String color = categoryColors.get(valueEntry.getKey());
				sb.append("<div title='").append(safeHtml(valueEntry.getKey())).append(": ").append(formatNumber(value))
						.append("' style='height:16px;width:").append(formatDecimal(width))
						.append("%;background:").append(color).append(";'></div>");
			}
			sb.append("</div>")
					.append("<div style='height:5px;margin-top:8px;background:#e2e8f0;border-radius:999px;overflow:hidden;'>")
					.append("<div style='height:5px;width:").append(formatDecimal(rowPercent))
					.append("%;background:linear-gradient(90deg,#0f766e,#22c55e);'></div></div>");

			sb.append("<div style='display:flex;gap:6px;flex-wrap:wrap;margin-top:8px;'>");
			for (Map.Entry<String, Number> valueEntry : rowEntry.getValue().entrySet()) {
				int value = valueEntry.getValue() == null ? 0 : valueEntry.getValue().intValue();
				if (value <= 0) {
					continue;
				}
				String color = categoryColors.get(valueEntry.getKey());
				sb.append("<span style='font-size:10px;border-radius:999px;padding:4px 7px;background:#fff;border:1px solid #e2e8f0;color:#475569;'>")
						.append("<b style='color:").append(color).append(";'>").append(formatNumber(value))
						.append("</b> ").append(safeHtml(valueEntry.getKey())).append("</span>");
			}
			sb.append("</div></div>");
		}
		sb.append("</div>");

		sb.append("<div style='margin-top:14px;padding:10px 12px;border-radius:14px;background:#eff6ff;border:1px solid #bfdbfe;")
				.append("font-size:11px;line-height:1.5;color:#1e3a8a;'>")
				.append("<b>Manfaat panel:</b> membantu membaca penyebaran surat antar satuan kerja dan kategori tanpa harus menghitung tabel secara manual. ")
				.append("Bar yang lebih panjang menunjukkan jumlah dokumen yang lebih banyak sehingga prioritas pemeriksaan lebih mudah terlihat.")
				.append("</div>");
		sb.append("</div>");
		return sb.toString();
	}

	private String safeHtml(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private String formatNumber(int value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatDecimal(double value) {
		try {
			return Common.numberFormat.get().format(value).replace(",", ".");
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

}
