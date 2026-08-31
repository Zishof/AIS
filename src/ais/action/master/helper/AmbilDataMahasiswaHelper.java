package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.CommonUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Kelas;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK berbentuk window modal untuk mendaftarkan mahasiswa ke satu {@link Perkuliahan}
 * (menambah baris {@link Detailperkuliahan}), dengan penegakan aturan akademik: pembatasan SKS
 * berdasarkan IP ({@link Common#checkPembatasanSKSBerdasarkanIP}), matakuliah prasyarat
 * ({@link Common#checkMatakuliahPrasyarat}), dan status pembayaran per semester/tahap
 * ({@link Common#checkStatusPembayaranMahasiswa}).
 *
 * <p>
 * Mendukung dua mode pemilihan: centang per baris, atau checkbox "pilih semua" yang — bila aktif —
 * memproses SEMUA hasil pencarian saat ini (bukan hanya yang tampil di halaman aktif) lewat
 * {@link #initCriteria(boolean)}, dengan pengecekan tambahan status mahasiswa harus
 * {@code AKTIF} sebelum diproses. Pencarian mendukung filter NIM (tunggal/rentang), nama, tahun
 * angkatan, fakultas, prodi, status mahasiswa semester berjalan, dan kelas. Mahasiswa yang sudah
 * terdaftar di perkuliahan (via {@link Detailperkuliahan} tanpa {@code ikutiPerkuliahan}) tampil
 * tercentang.
 * </p>
 */
public class AmbilDataMahasiswaHelper {

	private Perkuliahan perkuliahan;
	private MyGrid grid;

	private Textbox nim;
	private Textbox nama;
	private Decimalbox tahunangkatan;
	private Textbox dariNim;
	private Textbox sampaiNim;

	private Combobox searchstatusmahasiswa = new Combobox();

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Integer semesterPendek;

	private Paging paging;
	private MyCheckboxConfig checkboxAll;
	private AmbilDataKelasBanbox searchkelas;

	/**
	 * @param perkuliahan    kelas matakuliah tujuan pendaftaran mahasiswa
	 * @param semesterPendek penanda semester pendek, memengaruhi perhitungan SKS yang sudah diambil
	 *                       dan pengecekan pembayaran; boleh {@code null}
	 */
	public AmbilDataMahasiswaHelper(Perkuliahan perkuliahan, Integer semesterPendek) {
		this.perkuliahan = perkuliahan;
		this.semesterPendek = semesterPendek;

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	/**
	 * Listener yang divalidasi saat checkbox baris mahasiswa dicentang: menghitung semester
	 * mahasiswa saat ini ({@link CommonUtil#getSemester}), lalu bila semester tersebut cocok dengan
	 * semester {@link #perkuliahan} dan mahasiswa belum bayar (dicek lewat
	 * {@link Common#checkStatusPembayaranMahasiswa}), checkbox dibatalkan (uncheck) dan pesan
	 * peringatan ditampilkan. (Blok validasi bentrok jadwal matakuliah lain dinonaktifkan/dikomentari
	 * di kode.)
	 */
	class MahasiswaOnCheck implements EventListener {

		private Mahasiswa mahasiswa;
		private MyCheckboxConfig checkbox;

		public MahasiswaOnCheck(Mahasiswa mahasiswa, MyCheckboxConfig checkbox) {
			this.mahasiswa = mahasiswa;
			this.checkbox = checkbox;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			if (checkbox.isDisabled()) {
				return;
			}

			Boolean ganjil = CommonUtil.isNowSemensterGanjil();
			Integer semester = CommonUtil.getSemester(mahasiswa.getTahunangkatan(), ganjil,
					mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
			System.out.println("mahasiswa semester on check : " + semester);

			if (checkbox.isChecked() && perkuliahan.getSemester() != null) {

				// Session session = HibernateUtil.currentSession();
				// Detailperkuliahan perkuliahanLain = ((Detailperkuliahan)
				// session.createCriteria(Detailperkuliahan.class)
				// .add(Restrictions.isNull("ikutiPerkuliahan")).add(Restrictions.eq("mahasiswa",
				// mahasiswa))
				// .add(Restrictions.ne("perkuliahan", perkuliahan))
				//
				// .createCriteria("perkuliahan", Criteria.LEFT_JOIN)
				//
				// .add(Restrictions.eq("merupakanRemedial",
				// perkuliahan.getMerupakanRemedial()))
				//
				// .add(semesterPendek == null ?
				// Restrictions.isNull("statusSemesterPendek")
				// : Restrictions.isNotNull("statusSemesterPendek"))
				//
				// .add(Restrictions.eq("tahunAjaran",
				// perkuliahan.getTahunAjaran()))
				// .add(Restrictions.eq("semester", perkuliahan.getSemester()))
				// .add(Restrictions.eq("matakuliah",
				// perkuliahan.getMatakuliah())).setMaxResults(1)
				// .uniqueResult());
				// if (perkuliahanLain != null) {
				// checkbox.setChecked(false);
				// MyMessageboxConfig.show("Mahasiswa dengan NIM " +
				// mahasiswa.getNim() + " dan nama "
				// + mahasiswa.getNama()
				// + " tidak bisa dimasukkan ke jadwal perkuliahan ini, karena
				// dia sudah mengambil matakuliah "
				// + perkuliahanLain.getPerkuliahan().getMatakuliah().getNama()
				// + ", tahun akademik "
				// + perkuliahanLain.getPerkuliahan().getTahunAjaran() + ",
				// semester "
				// + perkuliahanLain.getSemester() + ", kelas " +
				// perkuliahanLain.getPerkuliahan().getKelas()
				// + ", dosen "
				// + (perkuliahanLain.getPerkuliahan().getDosen1() == null ? ""
				// : perkuliahanLain.getPerkuliahan().getDosen1().getNama())
				// + ", hari " + perkuliahanLain.getPerkuliahan().getHari() + ",
				// jam "
				// + perkuliahanLain.getPerkuliahan().getWaktuMulai() + " s.d "
				// + perkuliahanLain.getPerkuliahan().getWaktuSelesai()
				// + ". Solusinya, anda bisa men-transfer mahasiswa tersebut ke
				// jadwal perkuliahan ini.",
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.EXCLAMATION);
				// return;
				// }
			}

			Integer tahap = perkuliahan == null || perkuliahan.getKurikulumPunyaMatakuliah() == null
					|| perkuliahan.getKurikulumPunyaMatakuliah().getTahap() == null ? 0
							: perkuliahan.getKurikulumPunyaMatakuliah().getTahap();

			if (checkbox.isChecked() && perkuliahan.getSemester() != null
					&& semester.intValue() == perkuliahan.getSemester().intValue()
					&& !Common.checkStatusPembayaranMahasiswa(perkuliahan.getSemester(), tahap, mahasiswa, false,
							false)) {
				checkbox.setChecked(false);
				MyMessageboxConfig.show(
						"Mahasiswa dengan NIM " + mahasiswa.getNim() + " belum melakukan pembayaran di semester "
								+ perkuliahan.getSemester()
								+ (ConstantValues.aktifkanTahapan && tahap > 0 ? " tahap " + tahap : ""),
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}

		}

	}

	/**
	 * Perender baris grid untuk satu {@link Mahasiswa}: checkbox pilih (tercentang bila sudah
	 * terdaftar di {@link #perkuliahan}, atau bila checkbox "pilih semua" aktif), NIM, nama, tahun
	 * angkatan, dan kelas hasil sinkronisasi {@link Common#singkronkanKrsMahasiswa(Mahasiswa)}.
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);
			// checkbox.setDisabled(mahasiswa.getStatus() == null
			// || !mahasiswa.getStatus().getId()
			// .equals(ConstantValues.AKTIF.getId()));

			MahasiswaOnCheck mahasiswaOnCheck = new MahasiswaOnCheck(mahasiswa, checkbox);
			checkbox.addEventListener(Events.ON_CHECK, mahasiswaOnCheck);
			checkbox.setAttribute("mahasiswaOnCheck", mahasiswaOnCheck);
			Session session = HibernateUtil.currentSession();

			Integer jml = ((Number) session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.isNull("ikutiPerkuliahan")).setProjection(Projections.rowCount())
					.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("perkuliahan", perkuliahan))
					.uniqueResult()).intValue();

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);

			checkbox.setChecked(!jml.equals(0));

			if (!checkbox.isDisabled() && checkboxAll.isChecked()) {
				checkbox.setChecked(true);
			}

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

			new Label(krsMahasiswa.getKelas()).setParent(arg0);

		}
	}

	/**
	 * Mendaftarkan {@code mahasiswa} ke {@link #perkuliahan} bila belum terdaftar: menghitung
	 * semester dan SKS yang sudah diambil, menegakkan pembatasan SKS berdasarkan IP dan matakuliah
	 * prasyarat (menolak pendaftaran bila melanggar), lalu membuat {@link Detailperkuliahan} baru
	 * lewat {@link KrsUtilHelper#simpanKrsJikaBelumAda}.
	 *
	 * @param mahasiswa mahasiswa yang akan didaftarkan
	 * @return {@code true} bila berhasil (atau sudah terdaftar sebelumnya); {@code false} bila
	 *         ditolak karena melanggar pembatasan SKS atau prasyarat matakuliah
	 * @throws Exception diteruskan dari kegagalan Hibernate
	 */
	private boolean prosesSave(Mahasiswa mahasiswa) throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();

		Session session = HibernateUtil.currentNativeSession();
		Integer jml = ((Number) session.createCriteria(Detailperkuliahan.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("perkuliahan", perkuliahan))
				.uniqueResult()).intValue();
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		if (jml.equals(0)) {

			Integer tahun = Integer.parseInt(StringUtils.split(perkuliahan.getTahunAjaran(), "/")[0]);
			String ganjilGenap = perkuliahan.getGanjilGenap();
			Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), ganjilGenap,
					mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

			Integer jumlah = KrsUtilHelper.hitungSksYangTelahDiambil(null, mahasiswa, null, semester, semesterPendek);

			if (Common.checkPembatasanSKSBerdasarkanIP(mahasiswa, semester, jumlah, semesterPendek)) {
				return false;
			}

			if (!Common.checkMatakuliahPrasyarat(perkuliahan.getMatakuliah(), mahasiswa, semester)) {
				return false;
			}

			Detailperkuliahan detailperkuliahan = new Detailperkuliahan(tbmuser, AmbilDataMahasiswaHelper.class);
			detailperkuliahan.setSemester(semester);
			detailperkuliahan.setNilaiHuruf("");
			detailperkuliahan.setTotalNilai(0.0);
			detailperkuliahan.setMahasiswa(mahasiswa);
			detailperkuliahan.setPerkuliahan(perkuliahan);
			detailperkuliahan.setPersetujuan(Detailperkuliahan.BELUM_DISETUJUI);
			System.out.println("mahasiswa ini semester " + mahasiswa.getSemesterMulai());

			try {
				session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				KrsUtilHelper.simpanKrsJikaBelumAda(session, detailperkuliahan);
				session.getTransaction().commit();
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataMahasiswaHelper.java:278");
			}
			HibernateUtil.closeSession();
		}
		return true;
	}

	/**
	 * Mendaftarkan mahasiswa terpilih ke {@link #perkuliahan}. Bila checkbox "pilih semua"
	 * tercentang, memproses SEMUA hasil {@link #initCriteria(boolean)} (bukan hanya baris yang
	 * tampil), dengan tambahan pengecekan status mahasiswa harus {@code AKTIF} dan status pembayaran
	 * sebelum memanggil {@link #prosesSave(Mahasiswa)}; bila tidak, hanya baris grid yang
	 * checkbox-nya tercentang yang diproses.
	 *
	 * @return selalu {@code true}
	 * @throws Exception tidak pernah dilempar keluar dari cabang per-baris (ditangkap dan diaudit),
	 *                    namun dideklarasikan pada signature untuk cabang "pilih semua"
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean save() throws Exception {

		if (checkboxAll.isChecked()) {
			List<Mahasiswa> mahasiswas = initCriteria(true).list();

			Integer tahap = perkuliahan == null || perkuliahan.getKurikulumPunyaMatakuliah() == null
					|| perkuliahan.getKurikulumPunyaMatakuliah().getTahap() == null ? 0
							: perkuliahan.getKurikulumPunyaMatakuliah().getTahap();

			for (Mahasiswa mahasiswa : mahasiswas) {

				StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
				if (statusMahasiswa.getId().equals(ConstantValues.AKTIF.getId())) {

					if (!Common.checkStatusPembayaranMahasiswa(perkuliahan.getSemester(), tahap, mahasiswa, false,
							semesterPendek != null)) {
						MyMessageboxConfig.show(
								"Mahasiswa dengan NIM " + mahasiswa.getNim()
										+ " belum melakukan pembayaran di semester " + perkuliahan.getSemester()
										+ (tahap > 0 ? " tahap " + tahap : "")
										+ (semesterPendek != null ? " semester pendek" : ""),
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						continue;
					}

					if (!prosesSave(mahasiswa)) {
						continue;
					}
				}
			}
		} else {

			Rows rows = grid.getRows();
			List<Row> list = rows.getChildren();
			for (Row row : list) {
				List data = row.getChildren();
				try {
					MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
					if (checkbox.isChecked()) {
						Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");
						if (!prosesSave(mahasiswa)) {
							continue;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaHelper.java:330");
					// TODO: handle exception
				}
			}
		}

		return true;
	}

	/**
	 * Membangun window modal "Ambil Data Mahasiswa" berisi form filter lengkap dan grid mahasiswa
	 * berpaging dengan checkbox pilih-semua. Tombol "Simpan" memanggil {@link #save()}, memuat ulang
	 * data pemanggil, lalu menyembunyikan window.
	 *
	 * @param dataLoader callback muat-ulang data pemanggil setelah simpan
	 * @param window     window modal tempat UI dibangun
	 */
	public void display(final DataLoader dataLoader, final MyWindow window) {

		Common.clear(window);
		window.setTitle("Ambil Data Mahasiswa");
		window.setWidth("90%");
		window.setHeight("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("200px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);
		//
		//
		//
		//

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox());
		nim.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dari Nim"));
		row.appendChild(dariNim = new Textbox());
		dariNim.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai Nim"));
		row.appendChild(sampaiNim = new Textbox());
		sampaiNim.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunangkatan = new Decimalbox());
		tahunangkatan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		Common.insertComboDanSemua(searchstatusmahasiswa, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		row.appendChild(searchstatusmahasiswa);
		searchstatusmahasiswa.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(searchkelas = new AmbilDataKelasBanbox());
		searchkelas.setWidth("90%");
		searchkelas.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		South mySouth = new South();
		mySouth.setParent(myBorderlayout1);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(myCenter1);

		paging.setParent(mySouth);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		checkboxAll = new MyCheckboxConfig();
		column.appendChild(checkboxAll);
		checkboxAll.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {

					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");

						if (myCheckbox.isDisabled()) {
							continue;
						}

						myCheckbox.setChecked(checkboxAll.isChecked());
						if (!checkboxAll.isChecked()) {
							continue;
						}

						MahasiswaOnCheck mahasiswaOnCheck = (MahasiswaOnCheck) myCheckbox
								.getAttribute("mahasiswaOnCheck");

						mahasiswaOnCheck.onEvent(arg0);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaHelper.java:494");

					}

				}
			}
		});

		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelas");

		onSearchDefault(null);

		South south = new South();
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (save()) {
					window.setVisible(false);
				}
				dataLoader.loadData(null);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public Criteria initCriteria(boolean order) {
		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatusmahasiswa.getSelectedItem() == null ? null
				: searchstatusmahasiswa.getSelectedItem().getValue());

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (statusMahasiswa != null) {
			String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
					+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}
		Kelas kelas = (Kelas) (searchkelas.getAttribute("kelas"));
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));

		criteria.add(kelas != null && !kelas.getNama().trim().isEmpty()
				? Restrictions.ilike("kelas", kelas.getNama().trim(), MatchMode.EXACT)
				: Restrictions.sqlRestriction("true")).add(criteriaStatus)
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(dariNim.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ge("nim", dariNim.getValue()))
				.add(sampaiNim.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.le("nim", sampaiNim.getValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging(initCriteria(false), paging);

		List<Mahasiswa> mahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
