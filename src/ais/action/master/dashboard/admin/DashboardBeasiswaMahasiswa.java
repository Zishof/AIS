package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.maintenance.MainAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Beasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.JenisPenerimaBeasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.beasiswa.MahasiswaDaftarBeasiswa;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard beasiswa mahasiswa. Kelas ini memilih variasi data
 * atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Intbox mulai}, {@code Intbox sampai},
 * {@code Combobox formatNilai}, {@code Div center}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox searchreqstatus}, {@code int width}; inisialisasi/lifecycle ({@code init()});
 * pembacaan/pencarian ({@code reload()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardBeasiswaMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Intbox mulai;
	private Intbox sampai;
	private Combobox formatNilai;
	private Div center;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchreqstatus;
	private int width = 750;
	private int height = 100;

	public DashboardBeasiswaMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardBeasiswaMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Beasiswa Mahasiswa",
				"Jumlah penerima beasiswa per program studi, beserta grafiknya.");
		org.zkoss.zk.ui.Component saringanHost = hostPortal[0];
		center = (org.zkoss.zul.Div) hostPortal[1];

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(saringanHost);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Number m = (Number) HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.max("tahun")).uniqueResult();

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tahun Beasiswa"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		};

		mulai = new Intbox((m == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) : m.intValue()) - 7);
		mulai.setCols(2);
		hbox.appendChild(mulai);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));

		sampai = new Intbox(mulai.getValue() + 7);
		sampai.setCols(2);
		hbox.appendChild(sampai);

		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);

		formatNilai = new Combobox();
		row.appendChild(new MyLabelConfig("Beasiswa"));
		row.appendChild(formatNilai);
		Common.insertComboDanSemua(formatNilai, "nama", Beasiswa.class);
		formatNilai.setWidth("90%");

		formatNilai.addEventListener("onChange", eventListener);

		searchreqstatus = new Combobox();
		row.appendChild(new MyLabelConfig("Status"));
		row.appendChild(searchreqstatus);
		searchreqstatus.setWidth("90%");
		MyComboitemConfig comboitem = new MyComboitemConfig("Belum Disetujui");
		comboitem.setValue(MahasiswaDaftarBeasiswa.BELUM_DIPROSES);
		searchreqstatus.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Telah Disetujui");
		comboitem.setValue(MahasiswaDaftarBeasiswa.DITERIMA);
		searchreqstatus.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Ditolak");
		comboitem.setValue(MahasiswaDaftarBeasiswa.DITOLAK);
		searchreqstatus.appendChild(comboitem);

		comboitem = new MyComboitemConfig("=Status=");
		comboitem.setValue(null);
		searchreqstatus.appendChild(comboitem);
		searchreqstatus.setSelectedItem(comboitem);
		searchreqstatus.setReadonly(true);

		searchreqstatus.addEventListener("onChange", eventListener);

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		row.appendChild(new MyLabelConfig("Fakultas / Prodi"));
		hbox = new Hbox();
		hbox.setParent(row);
		hbox.appendChild(searchfakultas);
		hbox.appendChild(searchjurusan);
		searchfakultas.addEventListener("onChange", eventListener);
		searchjurusan.addEventListener("onChange", eventListener);
		searchfakultas.setCols(2);
		searchjurusan.setCols(2);



		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				reload();
			}
		});

	}

	@SuppressWarnings("unchecked")
	private void reload() {
		Common.clear(center);

		final Beasiswa beasiswa = (Beasiswa) (formatNilai.getSelectedItem() == null
				|| formatNilai.getSelectedItem().getValue() == null ? null : formatNilai.getSelectedItem().getValue());

		final List<JenisPenerimaBeasiswa> jenisPenerimaBeasiswas = beasiswa != null
				&& beasiswa.getJenisPenerimaBeasiswa() != null
						? new ArrayList<JenisPenerimaBeasiswa>()
						: HibernateUtil.currentSession().createCriteria(JenisPenerimaBeasiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.addOrder(Order.asc("nama")).list();

		if (beasiswa != null && beasiswa.getJenisPenerimaBeasiswa() != null) {
			jenisPenerimaBeasiswas.add(beasiswa.getJenisPenerimaBeasiswa());
		}

		final Map<Long, List<Object[]>> datas = new HashMap<Long, List<Object[]>>();
		Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		final List<Jurusan> jurusans = HibernateUtil.currentSession().createCriteria(Jurusan.class)
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", jur.getId()))
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fak))
				.addOrder(Order.asc("fakultas"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		final Integer status = (Integer) (searchreqstatus.getSelectedItem() == null ? null
				: searchreqstatus.getSelectedItem().getValue());

		final int mul = mulai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 7
				: mulai.getValue();
		final int sam = sampai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
				: sampai.getValue();

		final List<Integer> tas = new ArrayList<Integer>();
		for (int tahun = mul; tahun <= sam; tahun++) {
			tas.add(tahun);
		}

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				Grid grid = new Grid();
				grid.setSclass("dgrid");
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Fakultas");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jurusan");
				column.setParent(columns);

				column.setWidth("15%");

				for (final Integer tahunAjaran : tas) {
					column.setParent(columns);
					column = new MyColumnConfig(tahunAjaran.toString());
					column.setAlign("center");
					column.setParent(columns);
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				for (final JenisPenerimaBeasiswa jenisPenerimaBeasiswa : jenisPenerimaBeasiswas) {

					Group group = new ais.ui.util.MyGroupConfig(jenisPenerimaBeasiswa.getNama());
					group.setParent(rows);

					SimpleCategoryModel categoryModel = new SimpleCategoryModel();
					categoryModel.clear();

					for (final Jurusan jurusan : jurusans) {

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);
						row.appendChild(new MyLabelBoldAja(jurusan.getFakultas().getNama()));
						row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

						List<Object[]> data = datas.get(jurusan.getId());

						for (final Integer tahunAjaran : tas) {

							Number count = 0;
							for (Object[] o : data) {
								Object tahunangkatan = o[2];
								JenisPenerimaBeasiswa myJenisPenerimaBeasiswa = (JenisPenerimaBeasiswa) o[1];
								if (tahunangkatan != null && myJenisPenerimaBeasiswa != null
										&& tahunangkatan.toString().equalsIgnoreCase(tahunAjaran.toString())
										&& myJenisPenerimaBeasiswa.getId().equals(jenisPenerimaBeasiswa.getId())) {
									count = (Number) o[0];
									break;
								}
							}
							categoryModel.setValue(jurusan.getNama(), tahunAjaran, count);
							A a = new A(count + "");
							a.setStyle("font-size:12px;");
							a.setParent(row);
							a.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
											MahasiswaDaftarBeasiswa.class, new DataCriteriaWithColumn() {

												@Override
												public Object[] initCriteria(boolean order) {

													try {

														Criteria criteria = HibernateUtil.currentSession()
																.createCriteria(MahasiswaDaftarBeasiswa.class)

																.add(status == null ? Restrictions.sqlRestriction("1=1")
																		: Restrictions.eq("terima", status))

																.add(beasiswa == null
																		? Restrictions.sqlRestriction("true")
																		: Restrictions.eq("beasiswa", beasiswa))

																.createAlias("beasiswa", "beasiswa")
																.createAlias("mahasiswa", "mahasiswa")
																.add(Restrictions.eq("mahasiswa.jurusan", jurusan))
																.add(Restrictions.eq("beasiswa.jenisPenerimaBeasiswa",
																		jenisPenerimaBeasiswa))
																.add(Restrictions.eq("beasiswa.tahun", tahunAjaran));

														String[] contents = new String[] { "mahasiswa.nim",
																"mahasiswa.nama", "mahasiswa.jurusan.nama",
																"beasiswa.nama", "beasiswa.instansi", "beasiswa.tahun",
																"beasiswa.jenisPenerimaBeasiswa.nama", "terima",
																"tanggalDaftar", "memenuhiSyarat", "totalSkor",
																"keterangan" };

														return new Object[] { criteria, contents };

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
					}

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);

					row.setSpans(((sam - mul) + 2) + "");
					row.setAlign("center");

					row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Beasiswa Mahasiswa", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
				Session session = HibernateUtil.currentNativeSession();
				int i = 1;
				for (final Jurusan jurusan : jurusans) {
					label.setValue("Sedang memproses data di prodi " + jurusan.getNama() + " ("
							+ Common.numberFormat.get().format((i * 100.0) / jurusans.size()) + ")");
					i++;
					List<Object[]> data = session.createCriteria(MahasiswaDaftarBeasiswa.class)
							.add(status == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("terima", status))

							.add(beasiswa == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("beasiswa", beasiswa))

							.createAlias("beasiswa", "beasiswa").createAlias("mahasiswa", "mahasiswa")

							.add(Restrictions.eq("mahasiswa.jurusan", jurusan))
							.add(Restrictions.in("beasiswa.tahun", tas))

							.setProjection(Projections.projectionList().add(Projections.rowCount())
									.add(Projections.groupProperty("beasiswa.jenisPenerimaBeasiswa"))
									.add(Projections.groupProperty("beasiswa.tahun")))
							.list();

					datas.put(jurusan.getId(), data);
				}
				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
