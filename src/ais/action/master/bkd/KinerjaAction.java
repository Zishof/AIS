package ais.action.master.bkd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.BiodataDosenAction.ManagingProdiYangDiajar;
import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.DetailArtikelHelper;
import ais.action.master.penelitiandanpengabdian.helper.PengajuanPenelitianDanPengabdianHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.AsesorPegawai;
import ais.database.model.AsesorPenunjangKinerjaDosen;
import ais.database.model.Dosen;
import ais.database.model.KewajibanBebanDosen;
import ais.database.model.Pegawai;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PenunjangKinerjaDosen;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusKewajibanBebanDosen;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KinerjaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private ManagingProdiYangDiajar managingProdiYangDiajar = new ManagingProdiYangDiajar();

	private AmbilDataPegawaiBanbox pegawai;

	private North pilihDosen;

	private Tabpanel ringkasan;
	private Tabpanel bidangPendidikan;
	private Tabpanel bidangPenelitian;
	private Tabpanel bidangPengabdian;
	private Tabpanel bidangPenunjang;

	protected Rows rows = null;

	private Textbox asesi;

	private Pegawai currentPegawai;

	private String ta = null;

	private String smt = null;

	public void onBidangPenunjang(Event event) throws Exception {
		if (bidangPenunjang.getChildren().isEmpty()) {

			bidangPenunjang.setHeight("9000px");

			Pegawai myPegawai = (Pegawai) pegawai.getAttribute("pegawai");

			Dosen myDosen = myPegawai == null ? null : myPegawai.getDosen();

			MyInclude iframe = new MyInclude("/pages/master/penunjang_kinerja_dosen.zul?1=1"
					+ (myDosen == null ? "" : "&dosen=" + myDosen.getId())
					+ (myPegawai == null ? "" : "&pegawai=" + myPegawai.getId()));
			iframe.setParent(bidangPenunjang);
		}
	}

	public void onBidangPendidikan(Event event) throws Exception {

		if (bidangPendidikan.getChildren().isEmpty()) {

			bidangPendidikan.setHeight("9000px");

			final Pegawai myPegawai = (Pegawai) pegawai.getAttribute("pegawai");
			final Dosen myDosen = myPegawai == null ? null : myPegawai.getDosen();

			if (myPegawai != null && myDosen == null) {
				MyInclude iframe = new MyInclude("/pages/master/penunjang_kinerja_dosen.zul?jenis="
						+ PenunjangKinerjaDosen.PENDIDIKAN + (myDosen == null ? "" : "&dosen=" + myDosen.getId())
						+ (myPegawai == null ? "" : "&pegawai=" + myPegawai.getId()));
				iframe.setParent(bidangPendidikan);
			} else {

				ais.ui.util.MyButtonTabbox btnTabPendidikan = ais.ui.util.MyButtonTabbox.buat(bidangPendidikan, "100%", new int[] { 0 });

				btnTabPendidikan.tambahTabLazy(0, "Pengajaran", "/img/svg/chalkboard-teacher-light.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
						org.zkoss.zul.Tabpanel tp = managingProdiYangDiajar.init(myDosen, null);
						java.util.List<org.zkoss.zk.ui.Component> kids = new java.util.ArrayList<org.zkoss.zk.ui.Component>(tp.getChildren());
						for (org.zkoss.zk.ui.Component kid : kids) kid.setParent(panel);
					}
				});
				btnTabPendidikan.tambahTabLazy(1, "Pembimbing", "/img/svg/chalkboard-user.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
						new MyInclude("/pages/master/bkd/bimbingan_skripsi.zul" + (myDosen == null ? "" : "?dosen=" + myDosen.getId())).setParent(panel);
					}
				});
				btnTabPendidikan.tambahTabLazy(2, "Penguji", "/img/svg/pencil-square.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
						new MyInclude("/pages/master/bkd/penguji_skripsi.zul" + (myDosen == null ? "" : "?dosen=" + myDosen.getId())).setParent(panel);
					}
				});
				btnTabPendidikan.tambahTabLazy(3, "Pembimbing KKN", "/img/svg/users.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
						new MyInclude("/pages/master/kkn/kelompok_kkn.zul" + (myDosen == null ? "" : "?dosen=" + myDosen.getId())).setParent(panel);
					}
				});
				btnTabPendidikan.tambahTabLazy(4, "Pembimbing PKL", "/img/svg/user-business.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
						new MyInclude("/pages/master/pkl/kelompok_pkl.zul" + (myDosen == null ? "" : "?dosen=" + myDosen.getId())).setParent(panel);
					}
				});
				btnTabPendidikan.tambahTabLazy(5, "Penulis Buku", "/img/svg/book.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
						new MyInclude("/pages/master/buku_bahan_ajar.zul" + (myDosen == null ? "" : "?dosen=" + myDosen.getId())).setParent(panel);
					}
				});
				btnTabPendidikan.tambahTabLazy(6, "Bidang Pendidikan lain-nya", "/img/svg/three-dots.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
						new MyInclude("/pages/master/penunjang_kinerja_dosen.zul?jenis=" + PenunjangKinerjaDosen.PENDIDIKAN
								+ (myDosen == null ? "" : "&dosen=" + myDosen.getId())
								+ (myPegawai == null ? "" : "&pegawai=" + myPegawai.getId())).setParent(panel);
					}
				});
			}
		}
	}

	public void onBidangPenelitian(Event event) throws Exception {

		if (bidangPenelitian.getChildren().isEmpty()) {

			bidangPenelitian.setHeight("9000px");

			final Pegawai myPegawai = (Pegawai) pegawai.getAttribute("pegawai");
			final Dosen myDosen = myPegawai == null ? null : myPegawai.getDosen();

			if (myPegawai != null && myDosen == null) {
				MyInclude iframe = new MyInclude("/pages/master/penunjang_kinerja_dosen.zul?jenis="
						+ PenunjangKinerjaDosen.PENELITIAN + (myDosen == null ? "" : "&dosen=" + myDosen.getId())
						+ (myPegawai == null ? "" : "&pegawai=" + myPegawai.getId()));
				iframe.setParent(bidangPenelitian);
			} else {

				final Tbmuser tbmuser = (Tbmuser) (myDosen == null ? null
						: HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("dosen", myDosen)).addOrder(Order.desc("tanggal_dirubah"))
								.setMaxResults(1).uniqueResult());

				ais.ui.util.MyButtonTabbox btnTabPenelitian = ais.ui.util.MyButtonTabbox.buat(bidangPenelitian, "100%", new int[] { 0 });

				// Tab 0: Penelitian - load immediately
				{
					org.zkoss.zul.Div panelPenelitian = btnTabPenelitian.tambahTab(0, "Penelitian", "/img/svg/journal-bookmark.svg");
					PengajuanPenelitianDanPengabdianHelper pengajuanHelper = new PengajuanPenelitianDanPengabdianHelper();
					MyWindow addWindowPengajuan = new MyWindow();
					addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					pengajuanHelper.displayPengajuan(false, tbmuser == null ? null : tbmuser.getUserId(),
							PengumumanAkademis.UNTUK_DOSEN, null, panelPenelitian, addWindowPengajuan, ConstantValues.PENELITIAN, "8500px");
				}
				btnTabPenelitian.tambahTabLazy(1, "Publikasi Ilmiah", "/img/svg/journal-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
						DetailArtikelHelper detailArtikelHelper = new DetailArtikelHelper(myDosen);
						MyWindow addWindowPengajuan = new MyWindow();
						addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						detailArtikelHelper.displayPengajuan(false, tbmuser == null ? null : tbmuser.getUserId(),
								PengumumanAkademis.UNTUK_DOSEN, null, panel, addWindowPengajuan, "8500px");
					}
				});
				btnTabPenelitian.tambahTabLazy(2, "Bidang Penelitian lain-nya", "/img/svg/three-dots.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
						new MyInclude("/pages/master/penunjang_kinerja_dosen.zul?jenis=" + PenunjangKinerjaDosen.PENELITIAN
								+ (myDosen == null ? "" : "&dosen=" + myDosen.getId())
								+ (myPegawai == null ? "" : "&pegawai=" + myPegawai.getId())).setParent(panel);
					}
				});
			}
		}
	}

	public void onBidangPengabdian(Event event) throws Exception {

		if (bidangPengabdian.getChildren().isEmpty()) {

			bidangPengabdian.setHeight("9000px");

			final Pegawai myPegawai = (Pegawai) pegawai.getAttribute("pegawai");
			final Dosen myDosen = myPegawai == null ? null : myPegawai.getDosen();

			if (myPegawai != null && myDosen == null) {

				MyInclude iframe = new MyInclude("/pages/master/penunjang_kinerja_dosen.zul?jenis="
						+ PenunjangKinerjaDosen.PENGABDIAN + (myDosen == null ? "" : "&dosen=" + myDosen.getId())
						+ (myPegawai == null ? "" : "&pegawai=" + myPegawai.getId()));
				iframe.setParent(bidangPengabdian);
			} else {

				ais.ui.util.MyButtonTabbox btnTabPengabdian = ais.ui.util.MyButtonTabbox.buat(bidangPengabdian, "100%", new int[] { 0 });

				// Tab 0: Pengabdian - load immediately
				{
					org.zkoss.zul.Div panelPengabdian = btnTabPengabdian.tambahTab(0, "Pengabdian", "/img/svg/user-follow-line.svg");
					Tbmuser tbmuser = (Tbmuser) (myDosen == null ? null
							: HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("dosen", myDosen)).addOrder(Order.desc("tanggal_dirubah"))
									.setMaxResults(1).uniqueResult());
					PengajuanPenelitianDanPengabdianHelper pengajuanHelper = new PengajuanPenelitianDanPengabdianHelper();
					MyWindow addWindowPengajuan = new MyWindow();
					addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					pengajuanHelper.displayPengajuan(false, tbmuser == null ? null : tbmuser.getUserId(),
							PengumumanAkademis.UNTUK_DOSEN, null, panelPengabdian, addWindowPengajuan, ConstantValues.PENGABDIAN, "8500px");
				}
				btnTabPengabdian.tambahTabLazy(1, "Bidang Pengabdian lain-nya", "/img/svg/three-dots.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
						new MyInclude("/pages/master/penunjang_kinerja_dosen.zul?jenis=" + PenunjangKinerjaDosen.PENGABDIAN
								+ (myDosen == null ? "" : "&dosen=" + myDosen.getId())
								+ (myPegawai == null ? "" : "&pegawai=" + myPegawai.getId())).setParent(panel);
					}
				});
			}
		}
	}

	public void tampilRingkasan() throws Exception {
		if (ringkasan.getChildren().isEmpty()) {
			ringkasan.setHeight("9000px");
			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(ringkasan);
			borderlayout.setHeight("9000px");

			North north = new North();
			north.setParent(borderlayout);

			Toolbar hbox = new Toolbar();
			hbox.setParent(north);

			hbox.appendChild(new MyLabelConfig("Asesi"));

			asesi = new Textbox();
			asesi.setCols(4);
			hbox.appendChild(asesi);
			hbox.appendChild(new MyLabelConfig("Tahun Akademik"));
			final Combobox tahunAkademik = Common.generateTahunAjaran(null);
			tahunAkademik.setCols(4);
			hbox.appendChild(tahunAkademik);
			tahunAkademik.setReadonly(true);
			hbox.appendChild(new MyLabelConfig("Semester"));
			final Combobox semester = new Combobox();
			semester.setCols(2);
			hbox.appendChild(semester);
			semester.setReadonly(true);

			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			semester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			semester.appendChild(comboitem);

			Common.selectComboItem(semester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

			if (ta != null) {
				Common.selectComboItem(true, tahunAkademik, ta);
				tahunAkademik.setDisabled(true);
			}

			if (smt != null) {
				Common.selectComboItem(true, semester, smt);
				semester.setDisabled(true);
			}

			final Combobox tampilkan = new Combobox();
			tampilkan.setCols(2);
			hbox.appendChild(tampilkan);
			tampilkan.setReadonly(true);

			comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Tampilkan yang telah dinilai");
			comboitem.setValue(1);
			tampilkan.appendChild(comboitem);

			comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Tampilkan yang belum dinilai");
			comboitem.setValue(2);
			tampilkan.appendChild(comboitem);

			comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Tampilkan semua");
			comboitem.setValue(3);
			tampilkan.appendChild(comboitem);

			tampilkan.setSelectedItem(comboitem);
			tampilkan.setReadonly(true);

			final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Terdapat SKS Beban");
			checkboxConfig.setChecked(true);
			hbox.appendChild(checkboxConfig);

			final Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			EventListener eventListener = new EventListener() {

				@SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(center);
					Pegawai myPegawai = (Pegawai) pegawai.getAttribute("pegawai");

					Long dsn = myPegawai == null || myPegawai.getId() == null ? -1L : myPegawai.getId();
					Integer tmpl = (Integer) (tampilkan.getSelectedItem() == null ? 3
							: tampilkan.getSelectedItem().getValue());

					Session session = HibernateUtil.currentSession();
					List<AsesorPenunjangKinerjaDosen> asesorPenunjangKinerjaDosensTemp = session
							.createCriteria(AsesorPegawai.class).add(Restrictions.eq("pegawai", myPegawai))
							.createAlias("asesor", "asesor")
							.setProjection(Projections.property("asesor.asesorPenunjangKinerjaDosen"))
							.createAlias("asesor.asesorPenunjangKinerjaDosen", "asesorPenunjangKinerjaDosen")
							.add(Restrictions.eq("asesorPenunjangKinerjaDosen.aktif", true))
							.addOrder(Order.asc("asesorPenunjangKinerjaDosen.nama")).list();

					List<AsesorPenunjangKinerjaDosen> asesorPenunjangKinerjaDosens = new ArrayList<AsesorPenunjangKinerjaDosen>();
					for (AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen : asesorPenunjangKinerjaDosensTemp) {
						if (!asesorPenunjangKinerjaDosens.contains(asesorPenunjangKinerjaDosen)) {
							asesorPenunjangKinerjaDosens.add(asesorPenunjangKinerjaDosen);
						}
					}
					asesorPenunjangKinerjaDosensTemp = null;

					String sql = "select\n" + " aaa.id_dosen,\n" + " max(aaa.dosen) as dosen,\n"
							+ "aaa.bidang_kinerja,aaa.bidang,\n" + " aaa.judul as judul,\n"
							+ "avg(case when aaa.output ='" + AsesemenPenilaian.OUTPUT_SELESAI + "' or aaa.output ='"
							+ AsesemenPenilaian.OUTPUT_LANJUTKAN
							+ "' or aaa.output is null then aaa.sks_beban else 0 end) sks_beban,max(aaa.bukti) as bukti,max(aaa.masatugas) as masatugas,max(aaa.bukti_kinerja) as bukti_kinerja,\n";

					for (AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen : asesorPenunjangKinerjaDosens) {
						sql += "sum(case when aaa.asesor_id=" + asesorPenunjangKinerjaDosen.getId()
								+ " and (aaa.output ='" + AsesemenPenilaian.OUTPUT_SELESAI + "' or aaa.output ='"
								+ AsesemenPenilaian.OUTPUT_LANJUTKAN
								+ "' or aaa.output is null) then aaa.sks_nilai else 0 end) sks_asesor"
								+ asesorPenunjangKinerjaDosen.getId() + ",\n";
					}

					sql += "avg(case when aaa.output = '" + AsesemenPenilaian.OUTPUT_SELESAI + "' or aaa.output ='"
							+ AsesemenPenilaian.OUTPUT_LANJUTKAN
							+ "' or aaa.output is null then  aaa.sks_nilai else 0 end) as sks \n" + " from (\n"
							+ " \tselect\n" + "\ta.bidang as bidang_kinerja,\n" + " \ta.spesifikasi as bidang,\n"
							+ " \ta1.id id_dosen,\n" + "\ta1.nama as dosen,\n" + " \ta2.nama as asesor,\n"
							+ " \ta2.id as asesor_id,\n" + "\ta3.sks_kinerja sks_nilai,\n" + "\ta.sks_beban,\n" + " \n"
							+ " \ta.keterangan as judul, a.bukti, a.output, a.masatugas, a3.bukti as bukti_kinerja \n"
							+ " \n" + " \t from asesemen_penilaian a\n"
							+ "\tinner join pegawai a1 on (a.pegawai=a1.id)\n"
							+ "\tleft join penilaian_asesor a3 on (a3.asesemen_penilaian = a.id)\n"
							+ "\tleft join asesor a4 on (a3.asesor = a4.id)\n"
							+ "\tleft join asesor_penunjang_kinerja_dosen a2 on (a4.asesor_penunjang_kinerja_dosen=a2.id)\n"
							+ "\n" + " \t where a2.aktif and a.aktif "

//						+ "\n and (a.output = '"
//						+ AsesemenPenilaian.OUTPUT_SELESAI + "' or a.output = '" + AsesemenPenilaian.OUTPUT_LANJUTKAN
//						+ "' or a.output is null) "

							+ (currentPegawai == null || currentPegawai.getDosen() == null ? ""
									: " and a1.dosen = " + currentPegawai.getDosen().getId())
							+ " \n" + " \t\t and a.tahunakademik='" + tahunAkademik.getSelectedItem().getValue() + "'\n"
							+ " \t and (case when " + tmpl + "=3 then true when " + tmpl
							+ "=2 then a3.sks_kinerja<0.01 else a3.sks_kinerja>0.01 end)\t  "
							+ (asesi.getValue().trim().isEmpty() ? ""
									: " and (a1.nama ilike '%" + asesi.getValue().trim() + "%' or a1.mycode ilike '%"
											+ asesi.getValue().trim() + "%') ")
							+ "    and a.semester='" + semester.getSelectedItem().getValue() + "'\n"
							+ " \t\tand case when " + dsn + "=-1 then true else a.pegawai=" + dsn + " end\n"
							+ " ) aaa\n" + "group by aaa.id_dosen,aaa.bidang_kinerja,aaa.bidang,aaa.judul \n"
							+ (checkboxConfig.isChecked() ? " having avg(aaa.sks_beban) > 0.1 " : "")
							+ "order by aaa.id_dosen,aaa.bidang_kinerja,aaa.bidang,aaa.judul";

					System.out.println(sql);
					List<Object[]> data = session.createSQLQuery(sql).list();

					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setWidth("100%");
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig("Dosen");
					column.setParent(columns);
					column.setWidth(currentPegawai == null || currentPegawai.getDosen() == null ? "15%" : "0%");

					column = new MyColumnConfig("Bidang");
					column.setParent(columns);
					column.setWidth("10%");

					column = new MyColumnConfig("Kinerja");
					column.setParent(columns);

					column = new MyColumnConfig("SKS Beban");
					column.setAlign("right");
					column.setParent(columns);
					column.setWidth("7%");

					for (AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen : asesorPenunjangKinerjaDosens) {
						column = new MyColumnConfig(asesorPenunjangKinerjaDosen.getNama());
						column.setAlign("right");
						column.setWidth("7%");
						column.setParent(columns);
					}

					column = new MyColumnConfig("Rata-Rata");
					column.setWidth("7%");
					column.setAlign("right");
					column.setParent(columns);

					column = new MyColumnConfig("%");
					column.setWidth("5%");
					column.setAlign("right");
					column.setParent(columns);

					column = new MyColumnConfig("Min.");
					column.setWidth("4%");
					column.setAlign("right");
					column.setParent(columns);

					column = new MyColumnConfig("Max.");
					column.setWidth("4%");
					column.setAlign("right");
					column.setParent(columns);

					column = new MyColumnConfig("Kesimpulan");
					column.setWidth("8%");
					column.setParent(columns);

					rows = new Rows();
					rows.setParent(grid);

					String dosen = "";
					String dosenlama = "";

					String bidang = "";
					String bidanglama = "";
					String bdg = "";
					String bdgLama = "";

					Double sksBebanDosenTotal = 0.0;
					Double sksBebanBidang = 0.0;
					Double sksDosen = 0.0;
					Double sksBidang = 0.0;
					Boolean memenuhi = true;

					// StatusKewajibanBebanDosen statusKewajibanBebanDosen = null;
					KewajibanBebanDosen kewajibanBebanDosen = null;
					List<String> datasSemua = Collections.synchronizedList(new ArrayList());
					Pegawai pegawai = null;

					for (Object[] oData : data) {

						String dosenId = oData[0] == null ? "-1" : oData[0].toString();
						dosen = oData[1] == null ? "" : oData[1].toString();
						bdg = (oData[2] == null ? "" : oData[2].toString());
						bidang = (oData[2] == null ? "" : oData[2].toString()) + " oleh " + dosen;

						Double sksBebanDosen = ((Number) (oData[5] == null ? 0.0 : oData[5])).doubleValue();

						String bukti = (oData[6] == null ? "" : oData[6].toString());
						String masatugas = (oData[7] == null ? "" : oData[7].toString());
						String bukti_kinerja = (oData[8] == null ? "" : oData[8].toString());

						if (!bidang.equals(bidanglama) && !bidanglama.trim().equals("")) {
							Row row = new Row();row.setValign("top");
							row.setValign("top");
							row.setValign("top");row.setAttribute("bukti", bukti);
							row.setValign("top");row.setAttribute("bukti_kinerja", bukti_kinerja);
							row.setValign("top");row.setAttribute("masatugas", masatugas);
							row.setValign("top");row.setAttribute("bdg", bdgLama);
							row.setValign("top");row.setAttribute("pegawai", pegawai);
							row.setParent(rows);
							row.setSpans("3,1," + asesorPenunjangKinerjaDosens.size() + ",1,1,1,1,1");
							row.appendChild(new ais.ui.util.MyHtml(
									"<div>Total SKS bidang " + bidanglama + "</div>"));
							row.appendChild(new MyLabelBold(Common.numberFormat.get().format(sksBebanBidang)));
							row.appendChild(new MyLabelBold(""));
							row.appendChild(new MyLabelBold(Common.numberFormat.get().format(sksBidang)));

							Double persen = sksBebanBidang < 0.01 ? 0.0 : ((sksBidang * 100.0) / sksBebanBidang);
							row.appendChild(new Label(Common.numberFormat.get().format(persen) + " %"));

							System.out.println("bidanglama => " + bidanglama + ", kewajibanBebanDosen => "
									+ kewajibanBebanDosen + " bukti " + bukti);

							if (kewajibanBebanDosen != null) {

								if (bidanglama.toLowerCase().startsWith("pendidikan")) {

									datasSemua.remove("Pendidikan");

									row.appendChild(new MyLabelBold(
											Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSksPendidikan())));
									row.appendChild(new MyLabelBold(""));

									if (kewajibanBebanDosen.getMinimalSksPendidikan() <= sksBebanBidang) {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
									} else {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
										memenuhi = false;
									}

								} else if (bidanglama.toLowerCase().startsWith("penelitian")) {

									datasSemua.remove("Penelitian");

									row.appendChild(new MyLabelBold(
											Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSksPenelitian())));
									row.appendChild(new MyLabelBold(""));

									if (kewajibanBebanDosen.getMinimalSksPenelitian() <= sksBebanBidang) {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
									} else {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
										memenuhi = false;
									}

								} else if (bidanglama.toLowerCase().startsWith("pengabdian")) {

									datasSemua.remove("Pengabdian");

									row.appendChild(new MyLabelBold(
											Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSksPengabdian())));
									row.appendChild(new MyLabelBold(""));

									if (kewajibanBebanDosen.getMinimalSksPengabdian() <= sksBebanBidang) {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
									} else {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
										memenuhi = false;
									}

								} else if (bidanglama.toLowerCase().startsWith("penunjang")) {

									datasSemua.remove("Penunjang");

									row.appendChild(new MyLabelBold(
											Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSksPenunjang())));
									row.appendChild(new MyLabelBold(""));

									if (kewajibanBebanDosen.getMinimalSksPenunjang() <= sksBebanBidang) {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
									} else {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
										memenuhi = false;
									}

								} else {
									row.appendChild(new Label(""));
									row.appendChild(new Label(""));
									row.appendChild(new Label(""));
								}
							} else {
								row.appendChild(new Label(""));
								row.appendChild(new Label(""));
								row.appendChild(new Label(""));
							}

							sksBidang = 0.0;
							sksBebanBidang = 0.0;
						}

						if (!dosen.equals(dosenlama) && !dosenlama.trim().equals("")) {

							for (String d : new ArrayList<String>(datasSemua)) {
								Row row = new Row();row.setValign("top");
								row.setValign("top");
								row.setValign("top");row.setAttribute("masatugas", masatugas);
								row.setValign("top");row.setAttribute("bukti", bukti);
								row.setValign("top");row.setAttribute("bukti_kinerja", bukti_kinerja);
								row.setValign("top");row.setAttribute("bdg", d);
								row.setValign("top");row.setAttribute("pegawai", pegawai);
								row.setParent(rows);
								row.setSpans("3,1," + asesorPenunjangKinerjaDosens.size() + ",1,1,1,1,1");
								row.appendChild(new ais.ui.util.MyHtml(
										"<div>Total SKS bidang " + d + "</div>"));
								row.appendChild(new MyLabelBold(Common.numberFormat.get().format(0)));
								row.appendChild(new MyLabelBold(""));
								row.appendChild(new MyLabelBold(Common.numberFormat.get().format(0)));
								row.appendChild(new MyLabelBold(Common.numberFormat.get().format(0) + " %"));

								if (d.toLowerCase().startsWith("pendidikan")) {

									datasSemua.remove("Pendidikan");

									row.appendChild(new MyLabelBold(kewajibanBebanDosen == null ? ""
											: Common.numberFormat.get()
													.format(kewajibanBebanDosen.getMinimalSksPendidikan())));
									row.appendChild(new MyLabelBold(""));

									if (kewajibanBebanDosen != null
											&& kewajibanBebanDosen.getMinimalSksPendidikan() <= sksBebanBidang) {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
									} else {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
										memenuhi = false;
									}

								} else if (d.toLowerCase().startsWith("penelitian")) {

									datasSemua.remove("Penelitian");

									row.appendChild(new MyLabelBold(kewajibanBebanDosen == null ? ""
											: Common.numberFormat.get()
													.format(kewajibanBebanDosen.getMinimalSksPenelitian())));
									row.appendChild(new MyLabelBold(""));

									if (kewajibanBebanDosen != null
											&& kewajibanBebanDosen.getMinimalSksPenelitian() <= sksBebanBidang) {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
									} else {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
										memenuhi = false;
									}

								} else if (d.toLowerCase().startsWith("pengabdian")) {

									datasSemua.remove("Pengabdian");

									row.appendChild(new MyLabelBold(kewajibanBebanDosen == null ? ""
											: Common.numberFormat.get()
													.format(kewajibanBebanDosen.getMinimalSksPengabdian())));
									row.appendChild(new MyLabelBold(""));

									if (kewajibanBebanDosen != null
											&& kewajibanBebanDosen.getMinimalSksPengabdian() <= sksBebanBidang) {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
									} else {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
										memenuhi = false;
									}

								} else if (d.toLowerCase().startsWith("penunjang")) {

									datasSemua.remove("Penunjang");

									row.appendChild(new MyLabelBold(kewajibanBebanDosen == null ? ""
											: Common.numberFormat.get()
													.format(kewajibanBebanDosen.getMinimalSksPenunjang())));
									row.appendChild(new MyLabelBold(""));

									if (kewajibanBebanDosen != null
											&& kewajibanBebanDosen.getMinimalSksPenunjang() <= sksBebanBidang) {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
									} else {
										row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
										memenuhi = false;
									}

								} else {
									row.appendChild(new Label(""));
									row.appendChild(new Label(""));
									row.appendChild(new Label(""));
								}
							}

							Row row = new Row();row.setValign("top");
							row.setValign("top");
							row.setValign("top");row.setAttribute("masatugas", masatugas);
							row.setValign("top");row.setAttribute("bukti", bukti);
							row.setValign("top");row.setAttribute("bukti_kinerja", bukti_kinerja);
							row.setValign("top");row.setAttribute("bdg", bdgLama);
							row.setValign("top");row.setAttribute("pegawai", pegawai);
							row.setParent(rows);
							row.setSpans("3,1," + asesorPenunjangKinerjaDosens.size() + ",1,1,1,1,1");
							row.appendChild(new ais.ui.util.MyHtml(
									"<div>Total SKS " + dosenlama + "</div>"));
							row.appendChild(new MyLabelBold(Common.numberFormat.get().format(sksBebanDosenTotal)));
							row.appendChild(new MyLabelBold(""));
							row.appendChild(new MyLabelBold(Common.numberFormat.get().format(sksDosen)));

							Double persen = sksBebanDosenTotal < 0.01 ? 0.0 : ((sksDosen * 100.0) / sksBebanDosenTotal);
							row.appendChild(new Label(Common.numberFormat.get().format(persen) + " %"));

							if (kewajibanBebanDosen != null) {

								row.appendChild(new MyLabelBold(
										Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSks())));
								row.appendChild(new MyLabelBold(
										Common.numberFormat.get().format(kewajibanBebanDosen.getMaksimalSks())));

								if (memenuhi && kewajibanBebanDosen.getMinimalSks() <= sksBebanDosenTotal
										&& kewajibanBebanDosen.getMaksimalSks() >= sksBebanDosenTotal) {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
								} else {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
								}

							} else {
								row.appendChild(new Label(""));
								row.appendChild(new Label(""));
								row.appendChild(new Label(""));
							}

							sksDosen = 0.0;
							sksBebanDosenTotal = 0.0;

						}

						Row row = new Row();row.setValign("top");
						row.setValign("top");
						row.setValign("top");row.setAttribute("masatugas", masatugas);
						row.setValign("top");row.setAttribute("bukti", bukti);
						row.setValign("top");row.setAttribute("bukti_kinerja", bukti_kinerja);
						row.setValign("top");row.setAttribute("bdg", bdg);
						row.setValign("top");row.setAttribute("pegawai", pegawai);
						row.setValign("bottom");
						row.setParent(rows);
						if (!dosen.equals(dosenlama)) {
							Hbox hbox = new Hbox();
							hbox.setPack("end");
							hbox.setAlign("end");

							// statusKewajibanBebanDosen = null;
							kewajibanBebanDosen = null;
							memenuhi = true;

							datasSemua = Collections.synchronizedList(new ArrayList());
							datasSemua.add("Pendidikan");
							datasSemua.add("Penelitian");
							datasSemua.add("Pengabdian");
							datasSemua.add("Penunjang");

							pegawai = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), Long.parseLong(dosenId));
							row.setValign("top");row.setAttribute("pegawai", pegawai);
							if (pegawai != null) {
								CommonMedia.tampilkanGambarKecil(pegawai).setParent(hbox);
								Vbox vbox = new Vbox();
								hbox.appendChild(vbox);
								vbox.appendChild(new Label(pegawai.getNama()));
								if (pegawai != null && pegawai.getDosen() != null
										&& pegawai.getDosen().getStatusKewajibanBebanDosen() != null) {
									StatusKewajibanBebanDosen statusKewajibanBebanDosen = pegawai.getDosen()
											.getStatusKewajibanBebanDosen();
									vbox.appendChild(new Label(statusKewajibanBebanDosen.getNama()));

									kewajibanBebanDosen = (KewajibanBebanDosen) session
											.createCriteria(KewajibanBebanDosen.class)
											.add(Restrictions.eq("statusKewajibanBebanDosen",
													statusKewajibanBebanDosen))
											.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
								}
							}
							row.appendChild(hbox);
						} else {
							row.appendChild(new Label());
						}

						row.appendChild(new Label(
								bidang.equals(bidanglama) ? "" : (oData[2] == null ? "" : oData[2].toString())));

						String jdl = oData[4] == null || oData[4].toString().trim().isEmpty() ? bukti_kinerja
								: oData[4].toString();

						jdl = jdl + " (" + bidang + ", bukti : " + bukti + ", masa tugas : " + masatugas + ")";

						row.appendChild(new Label(jdl));
						row.appendChild(new Label(Common.numberFormat.get().format(sksBebanDosen)));

						Double total = 0.0;
						int i = 9;
						for (@SuppressWarnings("unused")
						AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen : asesorPenunjangKinerjaDosens) {
							total += ((Number) oData[i]).doubleValue();
							row.appendChild(new Label(oData[i] == null ? "" : Common.numberFormat.get().format(oData[i])));
							i++;
						}

						Double rataRata = total / asesorPenunjangKinerjaDosens.size();
						sksBebanDosenTotal += sksBebanDosen;
						sksBebanBidang += sksBebanDosen;
						sksDosen += rataRata;
						sksBidang += rataRata;

						row.appendChild(new Label(Common.numberFormat.get().format(rataRata)));

						Double persen = sksBebanDosen < 0.01 ? 0.0 : ((rataRata * 100.0) / sksBebanDosen);
						row.appendChild(new Label(Common.numberFormat.get().format(persen) + " %"));

						row.appendChild(new Label(""));
						row.appendChild(new Label(""));
						row.appendChild(new Label(""));

						row.setValign("top");row.setAttribute("sks_kinerja", rataRata);
						row.setValign("top");row.setAttribute("persen_kinerja", persen);

						dosenlama = dosen;
						bidanglama = bidang;
						bdgLama = bdg;
					}

					if (!bidanglama.trim().equals("")) {
						Row row = new Row();row.setValign("top");
						row.setValign("top");
						row.setValign("top");row.setAttribute("bdg", bdgLama);
						row.setValign("top");row.setAttribute("pegawai", pegawai);
						row.setParent(rows);
						row.setSpans("3,1," + asesorPenunjangKinerjaDosens.size() + ",1,1,1,1,1");
						row.appendChild(new ais.ui.util.MyHtml(
								"<div>Total SKS bidang " + bidanglama + "</div>"));
						row.appendChild(new MyLabelBold(Common.numberFormat.get().format(sksBebanBidang)));
						row.appendChild(new MyLabelBold(""));
						row.appendChild(new MyLabelBold(Common.numberFormat.get().format(sksBidang)));

						Double persen = sksBebanBidang < 0.01 ? 0.0 : ((sksBidang * 100.0) / sksBebanBidang);
						row.appendChild(new MyLabelBold(Common.numberFormat.get().format(persen) + " %"));

						System.out.println(
								"bidanglama => " + bidanglama + ", kewajibanBebanDosen => " + kewajibanBebanDosen);

						if (kewajibanBebanDosen != null) {

							if (bidanglama.toLowerCase().startsWith("pendidikan")) {

								datasSemua.remove("Pendidikan");

								row.appendChild(new MyLabelBold(
										Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSksPendidikan())));
								row.appendChild(new MyLabelBold(""));

								if (kewajibanBebanDosen.getMinimalSksPendidikan() <= sksBebanBidang) {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
								} else {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
									memenuhi = false;
								}

							} else if (bidanglama.toLowerCase().startsWith("penelitian")) {

								datasSemua.remove("Penelitian");

								row.appendChild(new MyLabelBold(
										Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSksPenelitian())));
								row.appendChild(new MyLabelBold(""));

								if (kewajibanBebanDosen.getMinimalSksPenelitian() <= sksBebanBidang) {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
								} else {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
									memenuhi = false;
								}

							} else if (bidanglama.toLowerCase().startsWith("pengabdian")) {

								datasSemua.remove("Pengabdian");

								row.appendChild(new MyLabelBold(
										Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSksPengabdian())));
								row.appendChild(new MyLabelBold(""));

								if (kewajibanBebanDosen.getMinimalSksPengabdian() <= sksBebanBidang) {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
								} else {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
									memenuhi = false;
								}

							} else if (bidanglama.toLowerCase().startsWith("penunjang")) {

								datasSemua.remove("Penunjang");

								row.appendChild(new MyLabelBold(
										Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSksPenunjang())));
								row.appendChild(new MyLabelBold(""));

								if (kewajibanBebanDosen.getMinimalSksPenunjang() <= sksBebanBidang) {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
								} else {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
									memenuhi = false;
								}

							} else {
								row.appendChild(new Label(""));
								row.appendChild(new Label(""));
								row.appendChild(new Label(""));
							}
						} else {
							row.appendChild(new Label(""));
							row.appendChild(new Label(""));
							row.appendChild(new Label(""));
						}

						sksBidang = 0.0;
						sksBebanBidang = 0.0;
					}

					if (!dosenlama.trim().equals("")) {

						for (String d : new ArrayList<String>(datasSemua)) {
							Row row = new Row();row.setValign("top");
							row.setValign("top");
							row.setValign("top");row.setAttribute("bdg", d);
							row.setValign("top");row.setAttribute("pegawai", pegawai);
							row.setParent(rows);
							row.setSpans("3,1," + asesorPenunjangKinerjaDosens.size() + ",1,1,1,1,1");
							row.appendChild(new ais.ui.util.MyHtml(
									"<div>Total SKS bidang " + d + "</div>"));
							row.appendChild(new MyLabelBold(Common.numberFormat.get().format(0)));
							row.appendChild(new MyLabelBold(""));
							row.appendChild(new MyLabelBold(Common.numberFormat.get().format(0)));
							row.appendChild(new MyLabelBold(Common.numberFormat.get().format(0) + " %"));

							if (d.toLowerCase().startsWith("pendidikan")) {

								datasSemua.remove("Pendidikan");

								row.appendChild(new MyLabelBold(
										Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSksPendidikan())));
								row.appendChild(new MyLabelBold(""));

								if (kewajibanBebanDosen.getMinimalSksPendidikan() <= sksBebanBidang) {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
								} else {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
									memenuhi = false;
								}

							} else if (d.toLowerCase().startsWith("penelitian")) {

								datasSemua.remove("Penelitian");

								row.appendChild(new MyLabelBold(
										Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSksPenelitian())));
								row.appendChild(new MyLabelBold(""));

								if (kewajibanBebanDosen.getMinimalSksPenelitian() <= sksBebanBidang) {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
								} else {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
									memenuhi = false;
								}

							} else if (d.toLowerCase().startsWith("pengabdian")) {

								datasSemua.remove("Pengabdian");

								row.appendChild(new MyLabelBold(
										Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSksPengabdian())));
								row.appendChild(new MyLabelBold(""));

								if (kewajibanBebanDosen.getMinimalSksPengabdian() <= sksBebanBidang) {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
								} else {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
									memenuhi = false;
								}

							} else if (d.toLowerCase().startsWith("penunjang")) {

								datasSemua.remove("Penunjang");

								row.appendChild(new MyLabelBold(
										Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSksPenunjang())));
								row.appendChild(new MyLabelBold(""));

								if (kewajibanBebanDosen.getMinimalSksPenunjang() <= sksBebanBidang) {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
								} else {
									row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
									memenuhi = false;
								}

							} else {
								row.appendChild(new Label(""));
								row.appendChild(new Label(""));
								row.appendChild(new Label(""));
							}
						}

						Row row = new Row();row.setValign("top");
						row.setValign("top");
						row.setValign("top");row.setAttribute("bdg", "Total");
						row.setValign("top");row.setAttribute("judul", "Total SKS " + dosenlama);
						row.setValign("top");row.setAttribute("pegawai", pegawai);
						row.setParent(rows);
						row.setSpans("3,1," + asesorPenunjangKinerjaDosens.size() + ",1,1,1,1,1");
						row.appendChild(new ais.ui.util.MyHtml(
								"<div>Total SKS " + dosenlama + "</div>"));
						row.appendChild(new MyLabelBold(Common.numberFormat.get().format(sksBebanDosenTotal)));
						row.appendChild(new MyLabelBold(""));
						row.appendChild(new MyLabelBold(Common.numberFormat.get().format(sksDosen)));

						Double persen = sksBebanDosenTotal < 0.01 ? 0.0 : ((sksDosen * 100.0) / sksBebanDosenTotal);
						row.appendChild(new Label(Common.numberFormat.get().format(persen) + " %"));

						if (kewajibanBebanDosen != null) {

							row.appendChild(
									new MyLabelBold(Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSks())));
							row.appendChild(
									new MyLabelBold(Common.numberFormat.get().format(kewajibanBebanDosen.getMaksimalSks())));

							if (memenuhi && kewajibanBebanDosen.getMinimalSks() <= sksBebanDosenTotal
									&& kewajibanBebanDosen.getMaksimalSks() >= sksBebanDosenTotal) {
								row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
							} else {
								row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak Memenuhi")));
							}

						} else {
							row.appendChild(new Label(""));
							row.appendChild(new Label(""));
							row.appendChild(new Label(""));
						}

						row = new Row();
						row.setValign("top");
						row.setValign("top");row.setAttribute("bdg", "Total");
						row.setValign("top");row.setAttribute("judul", "Kelebihan SKS " + dosenlama);
						row.setValign("top");row.setAttribute("pegawai", pegawai);
						row.setParent(rows);
						row.setSpans("3,1," + asesorPenunjangKinerjaDosens.size() + ",1,1,1,1,1");
						row.appendChild(new ais.ui.util.MyHtml(
								"<div>Kelebihan SKS " + dosenlama + "</div>"));
						row.appendChild(new MyLabelBold(""));
						row.appendChild(new MyLabelBold(""));
						row.appendChild(new MyLabelBold(""));

						row.appendChild(new Label(""));

						if (kewajibanBebanDosen != null) {

							if (kewajibanBebanDosen.getMaksimalSks() < sksDosen) {
								row.appendChild(new MyLabelBold(
										Common.numberFormat.get().format(sksDosen - kewajibanBebanDosen.getMinimalSks())));
								row.appendChild(new MyLabelBold(
										Common.numberFormat.get().format(sksDosen - kewajibanBebanDosen.getMaksimalSks())));
								row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelebihan")));
							} else {
								row.appendChild(new MyLabelBold(""));
								row.appendChild(new MyLabelBold(""));
								row.appendChild(new Label(ais.common.Common.getBahasaConfig("Memenuhi")));
							}

						} else {
							row.appendChild(new Label(""));
							row.appendChild(new Label(""));
							row.appendChild(new Label(""));
						}

						sksDosen = 0.0;
						sksBebanDosenTotal = 0.0;
					}

				}
			};

			eventListener.onEvent(null);
			tahunAkademik.addEventListener("onChange", eventListener);
			semester.addEventListener("onChange", eventListener);
			tampilkan.addEventListener("onChange", eventListener);
			checkboxConfig.addEventListener("onCheck", eventListener);
			asesi.addEventListener("onOK", eventListener);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
			button.addEventListener("onClick", eventListener);
			button.setParent(hbox);

			class MyBkd implements EventListener {

				private String nama;
				private String namaTab;

				public MyBkd(String nama, String namaTab) {
					this.nama = nama;
					this.namaTab = namaTab;
				}

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event arg0) throws Exception {
					List<Row> datas = rows.getChildren();
					List<Map> maps = new ArrayList<Map>();
					for (Row row : datas) {
						List c = row.getChildren();
						Map d = new HashMap();

						Pegawai pegawai = (Pegawai) row.getAttribute("pegawai");
						if (pegawai != null) {

							String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(pegawai));
							d.put("ta", tahunAkademik.getSelectedItem().getValue());
							d.put("semester", semester.getSelectedItem().getValue());

							d.put("nip", pegawai.getMycode());
							d.put("nidn", pegawai.getDosen() == null ? "" : pegawai.getDosen().getNidn());
							d.put("perguruan_tinggi",
									pegawai.getDosen() == null || pegawai.getDosen().getPerguruanTinggi() == null ? ""
											: pegawai.getDosen().getPerguruanTinggi().getNama());
							d.put("fakultas",
									pegawai.getDosen() == null || pegawai.getDosen().getFakultas() == null ? ""
											: pegawai.getDosen().getFakultas().getNama());
							d.put("jurusan", pegawai.getDosen() == null || pegawai.getDosen().getJurusan() == null ? ""
									: pegawai.getDosen().getJurusan().getNama());
							d.put("jabatan_fungsional_dosen",
									pegawai.getDosen() == null || pegawai.getDosen().getJabatanFungsionalDosen() == null
											? ""
											: pegawai.getDosen().getJabatanFungsionalDosen().getNama());
							d.put("golongan_pegawai",
									pegawai.getDosen() == null || pegawai.getDosen().getGolonganPegawai() == null ? ""
											: pegawai.getDosen().getGolonganPegawai().getNama());
							d.put("pendidikans2",
									pegawai.getDosen() == null || pegawai.getDosen().getPendidikans2() == null ? ""
											: pegawai.getDosen().getPendidikans2());
							d.put("pendidikans3",
									pegawai.getDosen() == null || pegawai.getDosen().getPendidikans3() == null ? ""
											: pegawai.getDosen().getPendidikans3());
							d.put("pendidikans1",
									pegawai.getDosen() == null || pegawai.getDosen().getPendidikans1() == null ? ""
											: pegawai.getDosen().getPendidikans1());

							d.put("bidang_ilmu",
									pegawai.getDosen() == null ? ""
											: pegawai.getDosen().getSpesialisasi1() + " "
													+ pegawai.getDosen().getSpesialisasi2() + " "
													+ pegawai.getDosen().getSpesialisasi3());

							d.put("telp", pegawai.getTelp());
							d.put("email", pegawai.getEmail());

							d.put("id_dosen", pegawai.getId());
							d.put("nama", pegawai.getNama());
							d.put("pegawai_jenis",
									pegawai.getDosen() == null
											|| pegawai.getDosen().getStatusKewajibanBebanDosen() == null ? ""
													: pegawai.getDosen().getStatusKewajibanBebanDosen().getNama());
							d.put("pegawai_foto", url);

							d.put("bidang_kinerja", row.getAttribute("bdg"));
							d.put("bukti", row.getAttribute("bukti"));
							d.put("bukti_kinerja", row.getAttribute("bukti_kinerja"));
							d.put("masatugas", row.getAttribute("masatugas"));
							d.put("masa_tugas", row.getAttribute("masatugas"));

							d.put("sks_kinerja", row.getAttribute("sks_kinerja"));
							d.put("persen_kinerja", row.getAttribute("persen_kinerja"));

							Dosen atasan = pegawai.getDosen().getAtasanlangsung() == null ? null
									: (Dosen) ConstantValues.ambil(Dosen.class.getName(),
											pegawai.getDosen().getAtasanlangsung());

							d.put("atasan", atasan == null ? "" : atasan.getNama());
							d.put("nip_atasan", atasan == null ? "" : atasan.getMycode());

							d.put("atasanlangsung", atasan == null || atasan.getId() == null ? -1L : atasan.getId());
							d.put("id_dosen_asli", pegawai.getDosen() == null ? -1L : pegawai.getDosen().getId());
							d.put("nomorsertifikasi",
									pegawai.getDosen() == null ? "" : pegawai.getDosen().getNomorSertifikasi());

							if (row.getAttribute("judul") != null && !row.getAttribute("judul").toString().isEmpty()) {
								d.put("judul", row.getAttribute("judul").toString());
							} else {

								try {
									Label label = (Label) c.get(2);
									d.put("judul", label.getValue());
								} catch (Exception e) {
									try {
										Html label = (Html) c.get(1);
										d.put("judul", label.getContent());
									} catch (Exception ew) { ais.common.ErrorAuditUtil.record(ew, "auto-audit(empty-catch) src/ais/action/master/bkd/KinerjaAction.java:1361");
										// TODO: handle exception
									}
								}
							}

							try {
								Label label = (Label) c.get(3);
								d.put("beban", label.getValue());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bkd/KinerjaAction.java:1370");
								// TODO: handle exception
							}

							try {
								Label label = (Label) c.get(c.size() - 5);
								d.put("kinerja", label.getValue());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bkd/KinerjaAction.java:1377");
								// TODO: handle exception
							}

							try {
								Label label = (Label) c.get(c.size() - 3);
								d.put("min", label.getValue());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bkd/KinerjaAction.java:1384");
								// TODO: handle exception
							}

							try {
								Label label = (Label) c.get(c.size() - 2);
								d.put("max", label.getValue());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bkd/KinerjaAction.java:1391");
								// TODO: handle exception
							}

							try {
								Label label = (Label) c.get(c.size() - 1);
								d.put("kesimpulan", label.getValue());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bkd/KinerjaAction.java:1398");
								// TODO: handle exception
							}

							if (pegawai.getDosen() != null) {
								Common.insertProperty(Dosen.class, pegawai.getDosen(), d, "dosen");
							}

							maps.add(d);
						}

					}

					System.out.println("maps => " + maps);

					Map parameters = ais.common.HashMapGenerator.getRand();
					parameters.put("ta", tahunAkademik.getSelectedItem().getValue());
					parameters.put("semester", semester.getSelectedItem().getValue());
					parameters.put("maps", maps);
					@SuppressWarnings("unused")
					Tabbox tabbox = Report.generatePDFReportKembaliTab(Report.PDF, new Map[] { parameters },
							new String[] { nama }, new String[] { namaTab }, ais.ui.util.WaktuUtil.getDate());

				}
			}

			button = new MyToolbarbuttonConfig("Beban", "/img/print.png");
			button.addEventListener("onClick", new MyBkd("form_rencana_beban_dosen_fix", "Beban"));
			button.setParent(hbox);

			button = new MyToolbarbuttonConfig("Kinerja", "/img/print.png");
			button.addEventListener("onClick", new MyBkd("form_rencana_kinerja_dosen_fix", "Kinerja"));
			button.setParent(hbox);
		}
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

		Tbmuser tbmuser = Common.getCurrentUser();

		if (execution.getParameter("pegawai") != null) {
			currentPegawai = (Pegawai) HibernateUtil.currentSession().createCriteria(Pegawai.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("pegawai")))).uniqueResult();
		} else {
			currentPegawai = tbmuser.ambilPegawai();
		}

		if (pilihDosen != null) { pilihDosen.setVisible(currentPegawai == null || currentPegawai.getDosen() == null); }

		if (pegawai != null && currentPegawai != null && currentPegawai.getId() != null) {
			pegawai.setAttribute("pegawai", currentPegawai);
			pegawai.setValue(currentPegawai.getNama());
			pegawai.setDisabled(true);
		}

		if (execution.getParameter("ta") != null) {
			ta = execution.getParameter("ta");

		}

		if (execution.getParameter("smt") != null) {
			smt = execution.getParameter("smt");

		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onBidangPendidikan(arg0);
				tampilRingkasan();
			}
		};

		if (pegawai != null) { pegawai.setEventListener(eventListener); }

		tampilRingkasan();

		// eventListener.onEvent(null);

	}

}
