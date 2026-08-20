package ais.action.master.helper.generic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.IsiAngketParameterUmumListener;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.IsiAngketParameterUmum;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.AngketPenilaianGuru;
import ais.database.model.sekolah.ChecklistBaruPenilaianGuruOlehSiswa;
import ais.database.model.sekolah.ChecklistPenilaianGuru;
import ais.database.model.sekolah.GrupChecklistPenilaianGuru;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AngketGuruWindow extends Groupbox {

	private static final long serialVersionUID = -8503828719463870174L;

	private Map<String, ChecklistBaruPenilaianGuruOlehSiswa> mapsKey = new HashMap<String, ChecklistBaruPenilaianGuruOlehSiswa>();
	private Map<String, Integer> jumlahChecklistCache = new HashMap<String, Integer>();
	private Map<String, Set<Long>> checklistAktifIdCache = new HashMap<String, Set<Long>>();
	private boolean masukanHarusDiisi;
	private List<Long> jadwalPelajarans;
	private Siswa siswa;
	private MyWindow addWindow;
	private boolean tampilClose;
	private String tahunAjaran;
	private String jenis;

	public AngketGuruWindow(String tahunAjaran, String jenis, List<Long> jadwalPelajarans, Siswa siswa,
			MyWindow addWindow, boolean tampilClose) {
		super();
		this.jadwalPelajarans = jadwalPelajarans;
		this.siswa = siswa;
		this.addWindow = addWindow;
		this.tampilClose = tampilClose;
		this.tahunAjaran = tahunAjaran;
		this.jenis = jenis;
		masukanHarusDiisi = Common.bolehKonfigurasi("masukan_penilaian_guru_harus_diisi", Konfigurasi.TIDAK_AKTIF);
		init(false);
	}

	@SuppressWarnings("unchecked")
	private void loadSavedMap(boolean refresh) {
		mapsKey.clear();
		if (siswa == null || siswa.getId() == null) {
			return;
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Criteria criteria = session.createCriteria(ChecklistBaruPenilaianGuruOlehSiswa.class)
					.createAlias("jadwalPelajaran", "jadwalPelajaran")
					.add(Restrictions.eq("siswa", siswa));
			if (jadwalPelajarans != null && !jadwalPelajarans.isEmpty()) {
				criteria.add(Restrictions.in("jadwalPelajaran.id", jadwalPelajarans));
			}
			List<ChecklistBaruPenilaianGuruOlehSiswa> rows = criteria.list();
			if (rows != null) {
				for (ChecklistBaruPenilaianGuruOlehSiswa row : rows) {
					if (row == null || row.getJadwalPelajaran() == null || row.getGuru() == null) {
						continue;
					}
					mapsKey.put(siswa.getId() + "_" + row.getJadwalPelajaran().getId() + "_" + row.getGuru().getId(), row);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketGuruWindow.java:126");
				}
			}
		}
	}

	private void init(boolean refresh) {
		Common.clear(this);
		setWidth("100%");
		setMold("3d");
		setStyle("border:0; background:#f8fafc; padding:0;");
		loadSavedMap(refresh);
		jumlahChecklistCache.clear();
		checklistAktifIdCache.clear();

		Div header = new Div();
		header.setParent(this);
		header.setStyle("padding:14px 16px; background:linear-gradient(135deg,#0f4c81,#2563eb); color:white; border-radius:10px 10px 0 0;");
		Label title = new Label("Angket Penilaian Guru Tahun Ajaran " + tahunAjaran + " " + jenis);
		title.setStyle("font-size:16px; font-weight:bold; color:white;");
		title.setParent(header);
		Label subtitle = new Label(ais.common.Common.getBahasaConfig("  Isi penilaian guru dengan jujur dan objektif."));
		subtitle.setStyle("display:block; margin-top:4px; font-size:11px; color:#dbeafe;");
		subtitle.setParent(header);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(this);
		/* FIX 20-08-2026: Toolbar ZK 5.5 dirender dengan overflow tersembunyi dan tinggi mengikuti isi bawaan,
		 * sehingga tombol "Selesai" yang diberi padding custom ikut terpotong. Tinggi dibuat otomatis,
		 * luapan ditampilkan, dan isinya dijaga tetap satu baris. */
		toolbar.setStyle("padding:8px 12px; background:#ffffff; border-bottom:1px solid #e5e7eb; height:auto; min-height:40px; overflow:visible; white-space:nowrap;");

		if (tampilClose) {
			MyToolbarbuttonConfig selesai = new MyToolbarbuttonConfig("Selesai", "/img/save.gif");
			selesai.setTooltiptext("Selesai mengisi angket");
			selesai.setParent(toolbar);
			selesai.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Clients.confirmClose(null);
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							String host = Common.getRequestHostWithProtocol();
							ExecutionsCtrl.getCurrent().sendRedirect((host == null || host.trim().isEmpty() ? "" : host) + "/main");
						}
					});
				}
			});
		}

		MyToolbarbuttonConfig refreshButton = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
		refreshButton.setParent(toolbar);
		refreshButton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				init(true);
			}
		});

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(this);
		grid.setStyle("border:0; background:#ffffff;");
		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig("Jadwal Pelajaran");
		column.setWidth("40%");
		column.setParent(columns);
		column = new MyColumnConfig("Guru");
		column.setParent(columns);

		ListModel model = new org.zkoss.zul.SimpleListModel(jadwalPelajarans == null ? new java.util.ArrayList<Long>() : jadwalPelajarans);
		grid.setRowRenderer(new ChecklistRenderer());
		grid.setModelCheckMobile(model);
	}

	class ChecklistRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) ConstantValues
					.ambil(JadwalPelajaran.class.getName(), (Serializable) data);
			row.setStyle("background:transparent;");
			new Label(jadwalPelajaran == null ? "" : jadwalPelajaran.infoSimple()).setParent(row);

			MyGrid guruGrid = new MyGrid();
			guruGrid.setHeight("100%");
			guruGrid.setWidth("100%");
			guruGrid.setParent(row);
			Rows rows = new Rows();
			rows.setParent(guruGrid);

			if (jadwalPelajaran == null) {
				new Label(ais.common.Common.getBahasaConfig("Jadwal tidak ditemukan")).setParent(row);
				return;
			}

			List<Guru> gurus = jadwalPelajaran.populateGuruBuNama();
			if (gurus == null || gurus.isEmpty()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak ada guru")).setParent(row);
				return;
			}
			Set<Long> sudahDitampilkan = new HashSet<Long>();
			for (Guru guru : gurus) {
				if (guru == null || guru.getId() == null || sudahDitampilkan.contains(guru.getId())) {
					continue;
				}
				sudahDitampilkan.add(guru.getId());
				createNewRowGuru(jadwalPelajaran, guru, rows);
			}
		}

		private void createNewRowGuru(final JadwalPelajaran jadwalPelajaran, final Guru guru, Rows rows) throws Exception {
			final Row row = new Row();
			row.setValign("top");
			row.setParent(rows);

			Vbox info = new Vbox();
			info.setWidth("100%");
			info.setParent(row);
			Hbox hbox = new Hbox();
			hbox.setParent(info);
			hbox.setAlign("center");

			Image image = new Image(CommonMedia.getUrlFotoPengguna(new Tbmuser(guru)));
			image.setWidth("54px");
			image.setHeight("54px");
			image.setStyle("border-radius:10px; border:1px solid #dbeafe; background:#fff; object-fit:cover;");
			A preview = new A();
			preview.appendChild(image);
			preview.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.previewGambar(CommonMedia.getUrlFotoPengguna(new Tbmuser(guru)));
				}
			});
			preview.setParent(hbox);
			Vbox texts = new Vbox();
			texts.setParent(hbox);
			new Label(guru.getNama() == null ? "Guru" : guru.getNama()).setParent(texts);
			new Label(jadwalPelajaran.infoSimple()).setParent(texts);

			final Label status = new Label();
			status.setParent(info);
			status.setStyle("font-size:11px; font-weight:bold; color:#334155; margin-top:4px;");

			final EventListener refreshStatus = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Set<Long> checklistAktifIds = ambilChecklistAktifIdsGuru(jadwalPelajaran, guru);
					Integer jumlahChecklist = Integer.valueOf(checklistAktifIds.size());
					String kodeUnik = siswa.getId() + "_" + jadwalPelajaran.getId() + "_" + guru.getId();
					ChecklistBaruPenilaianGuruOlehSiswa saved = mapsKey.get(kodeUnik);
					Integer jumlahSaved = hitungJumlahJawabanAktifGuru(saved, checklistAktifIds);
					Integer jumlahRiwayatNonAktif = hitungJumlahJawabanNonAktifGuru(saved, checklistAktifIds);
					status.setValue(formatStatusIsianAngketGuru(jumlahChecklist, jumlahSaved, jumlahRiwayatNonAktif));
					row.setStyle(jumlahChecklist.equals(jumlahSaved) ? "border:0; background:#fef9c3;" : "border:0; background:#ffffff;");
				}
			};

			MyButtonConfig button = new MyButtonConfig("Lakukan Penilaian", "/img/Check-icon.png");
			button.setParent(info);
			button.setWidth("180px");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(jadwalPelajaran, guru, refreshStatus);
					addWindow.setHeight("95%");
					addWindow.setWidth("95%");
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});
			refreshStatus.onEvent(null);
		}
	}


	private Integer hitungChecklist(JadwalPelajaran jadwalPelajaran, Guru guru) {
		return Integer.valueOf(ambilChecklistAktifIdsGuru(jadwalPelajaran, guru).size());
	}

	@SuppressWarnings("unchecked")
	private Set<Long> ambilChecklistAktifIdsGuru(JadwalPelajaran jadwalPelajaran, Guru guru) {
		String key = siswa == null || siswa.getId() == null || jadwalPelajaran == null || jadwalPelajaran.getId() == null
				|| guru == null || guru.getId() == null ? "GLOBAL" : siswa.getId() + "_" + jadwalPelajaran.getId()
						+ "_" + guru.getId();
		Set<Long> cached = checklistAktifIdCache.get(key);
		if (cached != null) {
			return cached;
		}
		Set<Long> result = new HashSet<Long>();
		try {
			Session session = HibernateUtil.currentSession();
			List<Long> ids = buildChecklistCriteria(session).setProjection(Projections.property("id")).list();
			if (ids != null) {
				for (Long id : ids) {
					if (id != null) {
						result.add(id);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		checklistAktifIdCache.put(key, result);
		jumlahChecklistCache.put(key, Integer.valueOf(result.size()));
		return result;
	}

	private Integer hitungJumlahJawabanAktifGuru(ChecklistBaruPenilaianGuruOlehSiswa hasil,
			Set<Long> checklistAktifIds) {
		if (hasil == null || checklistAktifIds == null || checklistAktifIds.isEmpty()) {
			return Integer.valueOf(0);
		}
		Integer count = Integer.valueOf(0);
		List<Object[]> values = hasil.ambilValue();
		for (Object[] obj : values) {
			Long id = ambilIdChecklistGuru(obj);
			if (id != null && checklistAktifIds.contains(id)) {
				count = Integer.valueOf(count.intValue() + 1);
			}
		}
		return count;
	}

	private Integer hitungJumlahJawabanNonAktifGuru(ChecklistBaruPenilaianGuruOlehSiswa hasil,
			Set<Long> checklistAktifIds) {
		if (hasil == null) {
			return Integer.valueOf(0);
		}
		Integer count = Integer.valueOf(0);
		List<Object[]> values = hasil.ambilValue();
		for (Object[] obj : values) {
			Long id = ambilIdChecklistGuru(obj);
			if (id != null && (checklistAktifIds == null || !checklistAktifIds.contains(id))) {
				count = Integer.valueOf(count.intValue() + 1);
			}
		}
		return count;
	}

	private Long ambilIdChecklistGuru(Object[] obj) {
		if (obj == null || obj.length == 0 || obj[0] == null) {
			return null;
		}
		try {
			return Long.valueOf(String.valueOf(obj[0]));
		} catch (Exception e) {
			return null;
		}
	}

	private String formatStatusIsianAngketGuru(Integer jumlahChecklist, Integer jumlahSaved,
			Integer jumlahRiwayatNonAktif) {
		int total = jumlahChecklist == null ? 0 : jumlahChecklist.intValue();
		int saved = jumlahSaved == null ? 0 : jumlahSaved.intValue();
		int riwayat = jumlahRiwayatNonAktif == null ? 0 : jumlahRiwayatNonAktif.intValue();
		String status = saved >= total ? "Telah diisi" : "Belum terisi";
		StringBuilder sb = new StringBuilder();
		sb.append(status).append(" - ").append(saved).append(" dari ").append(total).append(" isian aktif");
		if (riwayat > 0) {
			sb.append(", ").append(riwayat).append(" riwayat nonaktif");
		}
		return sb.toString();
	}

	
	private Criteria buildGrupCriteria(Session session) {
		Criteria criteria = session.createCriteria(GrupChecklistPenilaianGuru.class)
				.createAlias("angketPenilaianGuru", "angketPenilaianGuru", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNull("angketPenilaianGuru.untukSiswa"),
						Restrictions.eq("angketPenilaianGuru.untukSiswa", true)))
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Sekolah sekolah = siswa == null ? null : siswa.getSekolah();
		Yayasan yayasan = sekolah == null ? null : sekolah.getYayasan();
		if (sekolah != null) {
			criteria.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.sekolah", sekolah),
					Restrictions.isNull("angketPenilaianGuru.sekolah")));
		}
		if (yayasan != null) {
			criteria.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.yayasan", yayasan),
					Restrictions.isNull("angketPenilaianGuru.yayasan")));
		}
		String angkatan = siswa == null || siswa.getTahunMasuk() == null ? "" : siswa.getTahunMasuk().toString();
		criteria.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.angkatan", ""),
				Restrictions.or(Restrictions.ilike("angketPenilaianGuru.angkatan", angkatan),
						Restrictions.isNull("angketPenilaianGuru.angkatan"))));
		criteria.addOrder(Order.asc("angketPenilaianGuru.kode")).addOrder(Order.asc("isi"));
		return criteria;
	}

	private Criteria buildChecklistCriteria(Session session) {
		Criteria criteria = session.createCriteria(ChecklistPenilaianGuru.class)
				.createAlias("grupChecklistPenilaianGuru", "grupChecklistPenilaianGuru")
				.createAlias("grupChecklistPenilaianGuru.angketPenilaianGuru", "angketPenilaianGuru", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNull("angketPenilaianGuru.untukSiswa"),
						Restrictions.eq("angketPenilaianGuru.untukSiswa", true)))
				.add(Restrictions.or(Restrictions.eq("grupChecklistPenilaianGuru.aktif", true),
						Restrictions.isNull("grupChecklistPenilaianGuru.aktif")))
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Sekolah sekolah = siswa == null ? null : siswa.getSekolah();
		Yayasan yayasan = sekolah == null ? null : sekolah.getYayasan();
		if (sekolah != null) {
			criteria.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.sekolah", sekolah),
					Restrictions.isNull("angketPenilaianGuru.sekolah")));
		}
		if (yayasan != null) {
			criteria.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.yayasan", yayasan),
					Restrictions.isNull("angketPenilaianGuru.yayasan")));
		}
		String angkatan = siswa == null || siswa.getTahunMasuk() == null ? "" : siswa.getTahunMasuk().toString();
		criteria.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.angkatan", ""),
				Restrictions.or(Restrictions.ilike("angketPenilaianGuru.angkatan", angkatan),
						Restrictions.isNull("angketPenilaianGuru.angkatan"))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	private void init(final JadwalPelajaran jadwalPelajaran, final Guru guru, final EventListener eventListener)
			throws Exception {
		addWindow.setTitle("Penilaian Guru");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setStyle("overflow:hidden;");
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setStyle("overflow:auto; background:#f8fafc;");

		Vbox container = new Vbox();
		container.setWidth("100%");
		container.setStyle("padding:14px; background:#f8fafc;");
		container.setParent(center);

		Div card = new Div();
		card.setParent(container);
		card.setStyle("background:white; border:1px solid #dbeafe; border-radius:12px; padding:12px; box-shadow:0 6px 18px rgba(15,23,42,0.08);");

		Hbox header = new Hbox();
		header.setParent(card);
		header.setAlign("center");
		Image image = new Image(CommonMedia.getUrlFotoPengguna(new Tbmuser(guru)));
		image.setWidth("72px");
		image.setHeight("72px");
		image.setStyle("border-radius:12px; border:1px solid #dbeafe; background:white; object-fit:cover;");
		header.appendChild(image);
		Vbox guruInfo = new Vbox();
		guruInfo.setParent(header);
		Label namaGuru = new Label(guru.getNama() == null ? "Guru" : guru.getNama());
		namaGuru.setStyle("font-size:16px; font-weight:bold; color:#0f172a;");
		namaGuru.setParent(guruInfo);
		new Label(jadwalPelajaran.infoSimple()).setParent(guruInfo);

		Session session = HibernateUtil.currentSession();
		List<GrupChecklistPenilaianGuru> grupChecklist = ConstantValues.simpleList(buildGrupCriteria(session),
				GrupChecklistPenilaianGuru.class);

		String kodeUnik = siswa.getId() + "_" + jadwalPelajaran.getId() + "_" + guru.getId();
		final ChecklistBaruPenilaianGuruOlehSiswa savedAwal = getOrCreateChecklistGuru(siswa, guru, jadwalPelajaran);
		final Map<IsiAngketParameterUmum, Object[]> dataParameterTambahan = new HashMap<IsiAngketParameterUmum, Object[]>();
		final Textbox masukan = new Textbox(savedAwal == null ? "" : savedAwal.getMasukan());
		masukan.setRows(3);
		masukan.setWidth("100%");
		masukan.setStyle("box-sizing:border-box; border:1px solid #cbd5e1; border-radius:8px; padding:8px; background:#ffffff;");

		Long idAngket = null;
		Set<Long> checklistAktifDitampilkan = new HashSet<Long>();
		for (GrupChecklistPenilaianGuru grup : grupChecklist) {
			List<ChecklistPenilaianGuru> checklist = ConstantValues.simpleList(
					session.createCriteria(ChecklistPenilaianGuru.class).addOrder(Order.asc("isi"))
							.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
							.add(Restrictions.eq("grupChecklistPenilaianGuru", grup)),
					ChecklistPenilaianGuru.class);
			catatChecklistAktif(checklistAktifDitampilkan, checklist);
			if (checklist == null || checklist.isEmpty()) {
				continue;
			}
			AngketPenilaianGuru angket = grup.getAngketPenilaianGuru();
			if (angket != null && (idAngket == null || !idAngket.equals(angket.getId()))) {
				Div petunjuk = new Div();
				petunjuk.setParent(container);
				petunjuk.setStyle("margin-top:12px; background:#eff6ff; border:1px solid #bfdbfe; border-radius:12px; padding:12px;");
				petunjuk.appendChild(new MyCaptionStyled("Petunjuk"));
				String content = angket.getPetunjuk() == null ? "" : angket.getPetunjuk().replaceAll("\\n", "<br>");
				Html html = new ais.ui.util.MyHtml(content);
				html.setStyle("font-family:sans-serif; font-size:12px; color:#334155;");
				html.setParent(petunjuk);
				idAngket = angket.getId();
			}

			Div groupDiv = new Div();
			groupDiv.setParent(container);
			groupDiv.setStyle("margin-top:12px; background:white; border:1px solid #e5e7eb; border-radius:12px; padding:12px;");
			Label groupTitle = new Label((angket == null ? "" : angket.getIsi() + " - ") + grup.getIsi());
			groupTitle.setStyle("font-size:14px; font-weight:bold; color:#1e3a8a;");
			groupTitle.setParent(groupDiv);

			MyGrid gridChecklist = new MyGrid();
			gridChecklist.setWidth("100%");
			gridChecklist.setStyle("border:0; background:transparent; margin-top:8px;");
			gridChecklist.setParent(groupDiv);
			Columns columnsChecklist = new Columns();
			columnsChecklist.setParent(gridChecklist);
			MyColumnConfig col = new MyColumnConfig("Pertanyaan dan Penilaian");
			col.setWidth("100%");
			col.setParent(columnsChecklist);
			Rows rowsChecklist = new Rows();
			rowsChecklist.setParent(gridChecklist);

			Integer jumlahPilihan = angket == null ? Integer.valueOf(5) : angket.getJumlahPilihan();
			for (final ChecklistPenilaianGuru c : checklist) {
				final Row row = new Row();
				row.setValign("top");
				row.setStyle("border:0; background:#ffffff;");
				row.setParent(rowsChecklist);
				Vbox vbox = new Vbox();
				vbox.setWidth("100%");
				vbox.setStyle("box-sizing:border-box; padding:10px 12px; margin:0 0 8px 0; border:1px solid #e5e7eb; border-radius:10px; background:#f8fafc;");
				row.appendChild(vbox);
				Label pertanyaan = new Label(c.getIsi());
				pertanyaan.setStyle("font-weight:bold; color:#0f172a; line-height:18px; margin-bottom:6px; display:block;");
				pertanyaan.setParent(vbox);
				Integer nilaiTersimpan = savedAwal == null ? Integer.valueOf(0) : savedAwal.getValue(c);
				JSONObject pilihan = new JSONObject(c.getPilihan());
				final Radiogroup radiogroup = new Radiogroup();
				radiogroup.setStyle("display:block; margin-top:4px; margin-bottom:8px;");
				radiogroup.setParent(vbox);
				for (Integer i = 1; i <= jumlahPilihan.intValue(); i++) {
					MyRadioConfig radio = new MyRadioConfig(pilihan.isNull(i + "") ? i + "" : pilihan.getString(i + ""));
					radio.setValue(i.toString());
					radio.setAttribute("value", i);
					if (nilaiTersimpan != null) {
						radio.setSelected(nilaiTersimpan.equals(i));
					}
					radiogroup.appendChild(radio);
				}

				final Textbox keterangan = new Textbox(savedAwal == null ? "" : savedAwal.getKeteranganValue(c));
				keterangan.setWidth("100%");
				keterangan.setRows(2);
				keterangan.setStyle("box-sizing:border-box; border:1px solid #cbd5e1; border-radius:8px; padding:7px; background:#ffffff;");
				if (angket != null && angket.getTampilKeterangan()) {
					Label labelKeterangan = new Label(ais.common.Common.getBahasaConfig("Keterangan tambahan"));
					labelKeterangan.setStyle("display:block; margin:6px 0 4px 0; font-size:11px; font-weight:bold; color:#475569;");
					vbox.appendChild(labelKeterangan);
					vbox.appendChild(keterangan);
				}

				EventListener listener = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Integer nilai = Integer.valueOf(0);
						try {
							nilai = (Integer) radiogroup.getSelectedItem().getAttribute("value");
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketGuruWindow.java:579");
						}
						onSave(guru, jadwalPelajaran, c, nilai, keterangan.getValue().trim(), masukan.getValue().trim());
					}
				};
				radiogroup.addEventListener("onCheck", listener);
				keterangan.addEventListener("onChange", listener);
			}

			renderParameterTambahanGuru(session, grup, savedAwal, groupDiv, dataParameterTambahan);
		}

		renderChecklistGuruNonAktif(session, savedAwal, checklistAktifDitampilkan, container);

		Div masukanDiv = new Div();
		masukanDiv.setParent(container);
		masukanDiv.setStyle("margin-top:12px; background:#ffffff; border:1px solid #e5e7eb; border-radius:12px; padding:12px;");
		masukanDiv.appendChild(new Label("Masukan/Saran/Komentar " + (masukanHarusDiisi ? "*" : "")));
		masukan.setParent(masukanDiv);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.setTooltiptext("Tutup tanpa menyelesaikan penilaian");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					eventListener.onEvent(event);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketGuruWindow.java:batal");
				}
				addWindow.setVisible(false);
			}
		});
		batal.setParent(toolbar);
		MyToolbarbuttonConfig simpanTutup = new MyToolbarbuttonConfig("Simpan dan Tutup", "/img/save.gif");
		simpanTutup.setParent(toolbar);
		simpanTutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (masukanHarusDiisi && masukan.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Masukan/Saran/Komentar harus diisi", "Pemberitahuan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									masukan.focus();
								}
							});
					return;
				}
				if (!simpanParameterTambahan(dataParameterTambahan)) {
					return;
				}
				eventListener.onEvent(event);
				addWindow.setVisible(false);
			}
		});
		borderlayout.setParent(addWindow);
	}


	private void catatChecklistAktif(Set<Long> target, List<ChecklistPenilaianGuru> checklistPenilaianGurus) {
		if (target == null || checklistPenilaianGurus == null) {
			return;
		}
		for (ChecklistPenilaianGuru c : checklistPenilaianGurus) {
			if (c != null && c.getId() != null) {
				target.add(c.getId());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void renderChecklistGuruNonAktif(Session session, ChecklistBaruPenilaianGuruOlehSiswa hasil,
			Set<Long> checklistAktifDitampilkan, Vbox parent) {
		if (session == null || hasil == null || parent == null) {
			return;
		}
		List<Long> ids = ambilChecklistIdNonAktifDariJawabanGuru(hasil, checklistAktifDitampilkan);
		if (ids.isEmpty()) {
			return;
		}
		List<ChecklistPenilaianGuru> list = ConstantValues.simpleList(
				session.createCriteria(ChecklistPenilaianGuru.class)
						.createAlias("grupChecklistPenilaianGuru", "grupChecklistPenilaianGuru", Criteria.LEFT_JOIN)
						.add(Restrictions.in("id", ids)).addOrder(Order.asc("grupChecklistPenilaianGuru.id"))
						.addOrder(Order.asc("isi")),
				ChecklistPenilaianGuru.class);
		if (list == null || list.isEmpty()) {
			return;
		}

		Div card = new Div();
		card.setParent(parent);
		card.setStyle("margin-top:12px; background:#fff7ed; border:1px solid #fed7aa; border-radius:12px; "
				+ "padding:12px; box-shadow:0 4px 14px rgba(154,52,18,0.08);");
		Label title = new Label(ais.common.Common.getBahasaConfig("Riwayat Jawaban pada Pertanyaan yang Sudah Dinonaktifkan"));
		title.setStyle("display:block; font-size:13px; font-weight:bold; color:#9a3412; margin-bottom:6px;");
		title.setParent(card);
		Label desc = new Label(
				"Bagian ini hanya menampilkan jawaban lama agar siswa mengetahui bahwa pertanyaan tersebut pernah diisi. Nilai dan keterangan di bagian ini tidak dapat diubah karena pertanyaannya sudah tidak aktif.");
		desc.setStyle("display:block; font-size:11px; color:#9a3412; line-height:16px; margin-bottom:10px;");
		desc.setParent(card);

		for (ChecklistPenilaianGuru c : list) {
			if (c == null || c.getId() == null) {
				continue;
			}
			Div item = new Div();
			item.setStyle("padding:10px 12px; margin-bottom:8px; border:1px dashed #fdba74; border-radius:10px; background:#ffffff;");
			item.setParent(card);

			String grup = "";
			try {
				grup = c.getGrupChecklistPenilaianGuru() == null ? "" : c.getGrupChecklistPenilaianGuru().getIsi();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketGuruWindow.java:684");
			}
			Label q = new Label((grup == null || grup.trim().length() == 0 ? "" : grup + " - ")
					+ (c.getIsi() == null ? "Pertanyaan lama" : c.getIsi()));
			q.setStyle("display:block; font-weight:bold; color:#0f172a; line-height:18px;");
			q.setParent(item);

			Integer nilai = hasil.getValue(c);
			String ket = hasil.getKeteranganValue(c);
			Label nilaiLabel = new Label("Nilai tersimpan: " + (nilai == null ? "-" : String.valueOf(nilai)));
			nilaiLabel.setStyle("display:block; margin-top:5px; color:#334155; font-size:12px;");
			nilaiLabel.setParent(item);
			if (ket != null && ket.trim().length() > 0) {
				Label ketLabel = new Label("Keterangan: " + ket.trim());
				ketLabel.setStyle("display:block; margin-top:4px; color:#64748b; font-size:11px;");
				ketLabel.setParent(item);
			}
		}
	}

	private List<Long> ambilChecklistIdNonAktifDariJawabanGuru(ChecklistBaruPenilaianGuruOlehSiswa hasil,
			Set<Long> checklistAktifDitampilkan) {
		List<Long> ids = new ArrayList<Long>();
		if (hasil == null) {
			return ids;
		}
		List<Object[]> values = hasil.ambilValue();
		for (Object[] obj : values) {
			if (obj == null || obj.length == 0 || obj[0] == null) {
				continue;
			}
			Long id = null;
			try {
				id = Long.valueOf(String.valueOf(obj[0]));
			} catch (Exception e) {
				id = null;
			}
			if (id == null) {
				continue;
			}
			if (checklistAktifDitampilkan != null && checklistAktifDitampilkan.contains(id)) {
				continue;
			}
			if (!ids.contains(id)) {
				ids.add(id);
			}
		}
		return ids;
	}


	private ChecklistBaruPenilaianGuruOlehSiswa getOrCreateChecklistGuru(Siswa siswa, Guru guru,
			JadwalPelajaran jadwalPelajaran) {
		String kodeUnik = siswa.getId() + "_" + jadwalPelajaran.getId() + "_" + guru.getId();
		ChecklistBaruPenilaianGuruOlehSiswa data = mapsKey.get(kodeUnik);
		if (data != null && data.getId() != null) {
			return data;
		}
		Session session = HibernateUtil.currentSession();
		try {
			data = (ChecklistBaruPenilaianGuruOlehSiswa) session
					.createCriteria(ChecklistBaruPenilaianGuruOlehSiswa.class).add(Restrictions.eq("siswa", siswa))
					.add(Restrictions.eq("guru", guru)).add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
					.setMaxResults(1).uniqueResult();
			if (data == null) {
				data = new ChecklistBaruPenilaianGuruOlehSiswa();
				data.setSiswa(siswa);
				data.setGuru(guru);
				data.setJadwalPelajaran(jadwalPelajaran);
				data.setKeterangan("");
				data.setMasukan("");
				Common.refreshSaveOrUpdate(session, data);
				session.flush();
			}
			mapsKey.put(kodeUnik, data);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return data;
	}

	private void renderParameterTambahanGuru(Session session, GrupChecklistPenilaianGuru grup,
			ChecklistBaruPenilaianGuruOlehSiswa hasil, Div parent,
			Map<IsiAngketParameterUmum, Object[]> dataParameterTambahan) {
		if (session == null || grup == null || hasil == null || parent == null || hasil.getId() == null) {
			return;
		}
		try {
			Number jumlah = (Number) session.createCriteria(ais.database.model.ParameterTambahanAngketUmum.class)
					.add(Restrictions.eq("grupChecklistPenilaianGuru", grup)).setProjection(Projections.rowCount())
					.uniqueResult();
			if (jumlah == null || jumlah.intValue() <= 0) {
				return;
			}

			Div section = new Div();
			section.setStyle("width:100%; box-sizing:border-box; margin:10px 0 4px 0; padding:12px; background:#f8fafc; border:1px solid #cbd5e1; border-radius:12px;");
			section.setParent(parent);

			Label title = new Label(ais.common.Common.getBahasaConfig("Parameter Tambahan Angket Guru"));
			title.setStyle("display:block; font-size:13px; font-weight:bold; color:#334155; margin-bottom:8px;");
			title.setParent(section);

			MyGrid parameterGrid = new MyGrid();
			parameterGrid.setWidth("100%");
			parameterGrid.setStyle("border:0; background:transparent;");
			parameterGrid.setParent(section);

			Columns columns = new Columns();
			columns.setParent(parameterGrid);
			MyColumnConfig labelColumn = new MyColumnConfig();
			labelColumn.setWidth("28%");
			labelColumn.setParent(columns);
			MyColumnConfig inputColumn = new MyColumnConfig();
			inputColumn.setWidth("72%");
			inputColumn.setParent(columns);

			Rows parameterRowsContainer = new Rows();
			parameterRowsContainer.setParent(parameterGrid);

			IsiAngketParameterUmum isiAngketParameterUmum = getOrCreateIsiAngketParameterGuru(session, hasil);
			ArrayList<Row> parameterRows = new ArrayList<Row>();
			HashMap<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();
			IsiAngketParameterUmumListener listener = new IsiAngketParameterUmumListener(isiAngketParameterUmum,
					parameterRows, lampiranLains, parameterRowsContainer, grup);
			dataParameterTambahan.put(isiAngketParameterUmum, new Object[] { listener, lampiranLains });
			listener.onEvent(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private IsiAngketParameterUmum getOrCreateIsiAngketParameterGuru(Session session,
			ChecklistBaruPenilaianGuruOlehSiswa hasil) {
		IsiAngketParameterUmum isi = null;
		try {
			isi = (IsiAngketParameterUmum) session.createCriteria(IsiAngketParameterUmum.class)
					.add(Restrictions.eq("checklistBaruPenilaianGuruOlehSiswa", hasil)).setMaxResults(1)
					.uniqueResult();
			if (isi == null) {
				isi = new IsiAngketParameterUmum();
				isi.setNama(buildNamaIsiAngketParameterGuru(hasil));
				isi.setSiswa(siswa);
				isi.setGuru(hasil.getGuru());
				isi.setChecklistBaruPenilaianGuruOlehSiswa(hasil);
				isi.setParameterTambahan("");
				isi.setParameterTambahanInds("");
				Common.refreshSaveOrUpdate(session, isi);
				session.flush();
			} else if (isi.getNama() == null || isi.getNama().trim().isEmpty()) {
				isi.setNama(buildNamaIsiAngketParameterGuru(hasil));
				Common.refreshSaveOrUpdate(session, isi);
				session.flush();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return isi;
	}

	private String buildNamaIsiAngketParameterGuru(ChecklistBaruPenilaianGuruOlehSiswa hasil) {
		StringBuilder sb = new StringBuilder("Angket Parameter Guru");
		try {
			if (hasil != null && hasil.getGuru() != null && hasil.getGuru().getNama() != null) {
				sb.append(" - " ).append(hasil.getGuru().getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketGuruWindow.java:850");
		}
		try {
			if (siswa != null && siswa.getNamaSiswa() != null) {
				sb.append(" oleh " ).append(siswa.getNamaSiswa());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketGuruWindow.java:856");
		}
		try {
			if (hasil != null && hasil.getJadwalPelajaran() != null && hasil.getJadwalPelajaran().getId() != null) {
				sb.append(" / Jadwal #" ).append(hasil.getJadwalPelajaran().getId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketGuruWindow.java:862");
		}
		String value = sb.toString();
		return value.length() > 250 ? value.substring(0, 250) : value;
	}

	@SuppressWarnings("unchecked")
	private boolean simpanParameterTambahan(Map<IsiAngketParameterUmum, Object[]> dataParameterTambahan) {
		if (dataParameterTambahan == null || dataParameterTambahan.isEmpty()) {
			return true;
		}
		for (IsiAngketParameterUmum isiAngketParameterUmum : dataParameterTambahan.keySet()) {
			Object[] objects = dataParameterTambahan.get(isiAngketParameterUmum);
			IsiAngketParameterUmumListener listener = (IsiAngketParameterUmumListener) objects[0];
			try {
				if (!listener.validate()) {
					return false;
				}
				listener.onSave(isiAngketParameterUmum);
				HashMap<String, LampiranLain> lampiranLains = (HashMap<String, LampiranLain>) objects[1];
				if (lampiranLains != null && !lampiranLains.isEmpty()) {
					simpanLampiranParameter(isiAngketParameterUmum, lampiranLains);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				return false;
			}
		}
		return true;
	}

	private void simpanLampiranParameter(IsiAngketParameterUmum isiAngketParameterUmum,
			HashMap<String, LampiranLain> lampiranLains) {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				if (lampiranLain == null) {
					continue;
				}
				session.refresh(lampiranLain);
				lampiranLain.setRef(isiAngketParameterUmum.getId());
				session.update(lampiranLain);
			}
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketGuruWindow.java:911");}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AngketGuruWindow.java:916");}
			}
		}
	}

	public boolean onSave(Guru guru, JadwalPelajaran jadwalPelajaran, ChecklistPenilaianGuru checklistPenilaianGuru,
			Integer nilai, String keterangan, String masukan) throws Exception {
		Session session = HibernateUtil.currentSession();
		String kodeUnik = siswa.getId() + "_" + jadwalPelajaran.getId() + "_" + guru.getId();
		try {
			ChecklistBaruPenilaianGuruOlehSiswa data = mapsKey.get(kodeUnik);
			if (data == null) {
				data = new ChecklistBaruPenilaianGuruOlehSiswa();
			}
			data.setValue(nilai, siswa, guru, jadwalPelajaran, checklistPenilaianGuru, keterangan);
			data.setMasukan(masukan);
			Common.refreshSaveOrUpdate(session, data);
			session.flush();
			mapsKey.put(kodeUnik, data);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			init(true);
		}
		return true;
	}
}
