package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.pkl.KomponenPenilaianPkl;
import ais.database.model.pkl.PklPunyaKomponenPenilaianPkl;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PenilaianPklHelper implements DataLoader {

	private MyGrid grid;
	private KelompokPkl kelompokPkl;
	private Paging paging;
	private Textbox nim;
	private Tbmuser tbmuser;

	class DetailKelompokPklRenderer extends ais.ui.util.MyRowRenderer {

		Tbmuser tbmuser = Common.getCurrentUser();

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl = (MahasiswaDapatKelompokPkl) data;

			Mahasiswa mahasiswa = mahasiswaDapatKelompokPkl.getMahasiswa();

			Detailperkuliahan detailperkuliahan = mahasiswaDapatKelompokPkl.getDetailperkuliahan();

			if (detailperkuliahan == null) {
				Session sessionRender = HibernateUtil.currentSession();
				// KE-FIX (HibernateException "createCriteria is not valid without active
				// transaction"): dipanggil dari Grid renderer (Grid.doInitRenderer), yang bisa
				// jalan tanpa transaksi aktif pada session request ini.
				if (sessionRender.getTransaction() == null || !sessionRender.getTransaction().isActive()) {
					sessionRender.beginTransaction();
				}
				detailperkuliahan = (Detailperkuliahan) sessionRender
						.createCriteria(Detailperkuliahan.class).add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
						.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)
						.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
						.add(Restrictions.or(
								Restrictions.ilike("matakuliah.nama", Common.getBahasaConfig("pkl"),
										MatchMode.ANYWHERE),
								Restrictions.ilike("matakuliahKonversi.nama", Common.getBahasaConfig("pkl"),
										MatchMode.ANYWHERE)))
						.addOrder(Order.desc("semester")).setMaxResults(1).uniqueResult();
				mahasiswaDapatKelompokPkl.setDetailperkuliahan(detailperkuliahan);
				Common.refreshUpdate(mahasiswaDapatKelompokPkl);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(vbox);

			RevisiHelper.createNewRevisi(MahasiswaDapatKelompokPkl.class, mahasiswaDapatKelompokPkl, mahasiswa.getNim())
					.setParent(vbox);

			new Label(mahasiswa.getNama()).setParent(row);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(row);
			new Label(mahasiswa.getJurusan() == null ? ""
					: mahasiswa.getJurusan().getFakultas() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getNama())
					.setParent(row);

			if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.ambilDosen() == null) {
				final AmbilDataDetailPerkuliahanBanbox ambilDataMatakuliahBanbox = new AmbilDataDetailPerkuliahanBanbox(
						mahasiswa);
				ambilDataMatakuliahBanbox.setParent(row);
				ambilDataMatakuliahBanbox.setWidth("90%");
				ambilDataMatakuliahBanbox.setValue(detailperkuliahan == null ? ""
						: detailperkuliahan.getPerkuliahan() != null
								? detailperkuliahan.getPerkuliahan().getMatakuliah().getNama()
								: detailperkuliahan.getMatakuliahKonversi() != null
										? detailperkuliahan.getMatakuliahKonversi().getNama()
										: "");
				ambilDataMatakuliahBanbox.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						mahasiswaDapatKelompokPkl.setDetailperkuliahan(
								(Detailperkuliahan) ambilDataMatakuliahBanbox.getAttribute("detailperkuliahan"));
						Common.refreshUpdate(mahasiswaDapatKelompokPkl);
					}
				});
			} else {
				new Label(detailperkuliahan == null ? ""
						: detailperkuliahan.getPerkuliahan() != null
								? detailperkuliahan.getPerkuliahan().getMatakuliah().getNama()
								: detailperkuliahan.getMatakuliahKonversi() != null
										? detailperkuliahan.getMatakuliahKonversi().getNama()
										: "")
						.setParent(row);
			}

			boolean tidaksama = tbmuser != null && tbmuser.getMahasiswa() != null
					&& !tbmuser.getMahasiswa().getId().equals(mahasiswa.getId());

			if (tidaksama) {
				new Label("-").setParent(row);
				new Label("-").setParent(row);
				new Label("-").setParent(row);
			} else {
				new Label(mahasiswaDapatKelompokPkl.getTotalNilai() == null ? ""
						: Common.numberFormat.get().format(mahasiswaDapatKelompokPkl.getTotalNilai())).setParent(row);

				new Label(mahasiswaDapatKelompokPkl.getNilaiHuruf()).setParent(row);

				Hbox toolbar = new Hbox();
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Penilaian", "/img/svg/edit-box-line.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						init(mahasiswaDapatKelompokPkl);

					}

				});
				button.setParent(toolbar);
				toolbar.setParent(row);
			}

		}

	}

	@SuppressWarnings({ "unchecked" })
	private void init(final MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl) throws Exception {
		final MyWindow addWindow = new MyWindow();
		addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Tbmuser tbmuser = Common.getCurrentUser();

		addWindow.setTitle("Penilaian Pkl");
		addWindow.setWidth("850px");
		addWindow.setHeight("98%");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		addWindow.appendChild(borderlayout);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid subGrid = new MyGrid();
		subGrid.setWidth("100%");
		subGrid.setParent(center);
		subGrid.setHeight("100%");

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Komponen Penilaian"));

		Column column = new Column("Keterangan Komponen Penilaian");
		column.setWidth("40%");
		subColumns.appendChild(column);

		column = new Column("Bobot");
		column.setWidth("8%");
		subColumns.appendChild(column);

		column = new Column("Nilai");
		column.setWidth("10%");
		column.setAlign("right");
		subColumns.appendChild(column);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		String kolom = null;
		try {
			if (tbmuser.ambilDosen() != null) {
				TreeMap<String, Dosen> dosens = kelompokPkl.populateDosen();
				for (String key : dosens.keySet()) {
					try {
						Dosen d = dosens.get(key);
						if (d.getId().equals(tbmuser.getDosen().getId())) {
							kolom = key;
							break;
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianPklHelper.java:236");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianPklHelper.java:241");
		}

		final Footer footerTotal = new Footer(mahasiswaDapatKelompokPkl.getTotalNilai() == null ? ""
				: (Common.numberFormat.get().format(mahasiswaDapatKelompokPkl.getTotalNilai()) + " ("
						+ mahasiswaDapatKelompokPkl.getNilaiHuruf() + ")"));

		Session session = HibernateUtil.currentSession();
		List<KomponenPenilaianPkl> pklPunyaKomponenPenilaianPkls = session
				.createCriteria(PklPunyaKomponenPenilaianPkl.class)
				.setProjection(Projections.groupProperty("komponenPenilaianPkl"))
				.createAlias("komponenPenilaianPkl", "komponenPenilaianPkl")
				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianPkl.aktif"),
						Restrictions.eq("komponenPenilaianPkl.aktif", true)))
				.add(Restrictions.eq("pkl", kelompokPkl.getPkl())).list();
		TreeMap<KomponenPenilaianPkl, List<KomponenPenilaianPkl>> dataKomponenPenilaian = new TreeMap<KomponenPenilaianPkl, List<KomponenPenilaianPkl>>();
		for (KomponenPenilaianPkl komponenPenilaianPkl : pklPunyaKomponenPenilaianPkls) {
			if (komponenPenilaianPkl.getParent() != null) {
				if (!dataKomponenPenilaian.keySet().contains(komponenPenilaianPkl.getParent())) {
					List<KomponenPenilaianPkl> datas = new ArrayList<KomponenPenilaianPkl>();
					datas.add(komponenPenilaianPkl);
					dataKomponenPenilaian.put(komponenPenilaianPkl.getParent(), datas);
				} else {
					dataKomponenPenilaian.get(komponenPenilaianPkl.getParent()).add(komponenPenilaianPkl);
				}
			}
		}

		for (KomponenPenilaianPkl komponenPenilaianPkl : pklPunyaKomponenPenilaianPkls) {
			if (komponenPenilaianPkl.getParent() == null && !dataKomponenPenilaian.containsKey(komponenPenilaianPkl)) {
				List<KomponenPenilaianPkl> datas = new ArrayList<KomponenPenilaianPkl>();
				dataKomponenPenilaian.put(komponenPenilaianPkl, datas);
			}
		}
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(KomponenPenilaianPkl.class);
		for (final KomponenPenilaianPkl parent : dataKomponenPenilaian.keySet()) {

			final List<KomponenPenilaianPkl> datas = dataKomponenPenilaian.get(parent);
			if (datas.isEmpty()) {

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(subRows);
				row.appendChild(new Label(parent.getNama()));
				row.appendChild(new MyLabelAgakKecil(parent.getKeterangan()));
				row.appendChild(new Label(Common.numberFormat.get().format(parent.getBobot())));

				Boolean bolehMenilai = true;

				try {
					bolehMenilai = (Boolean) (kolom == null ? true
							: classMetadata.getPropertyValue(parent, kolom, EntityMode.POJO));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianPklHelper.java:293");

				}

				if (tbmuser.getMahasiswa() != null || !bolehMenilai) {
					row.appendChild(new Label(
							Common.numberFormat.get().format(mahasiswaDapatKelompokPkl.retreiveDetailNilai(parent))));
				} else {
					final MyDoublebox nilai = new MyDoublebox(mahasiswaDapatKelompokPkl.retreiveDetailNilai(parent));
					nilai.setWidth("90%");
					row.appendChild(nilai);

					nilai.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							mahasiswaDapatKelompokPkl.populateDetailNilai(parent, nilai.getValue(), true);
							mahasiswaDapatKelompokPkl.setTotalNilai(mahasiswaDapatKelompokPkl.hitungTotalNilai(true));
							Double total = mahasiswaDapatKelompokPkl.getTotalNilai();

							Detailperkuliahan detailperkuliahan = mahasiswaDapatKelompokPkl.getDetailperkuliahan();
							Matakuliah matakuliah = detailperkuliahan == null ? null
									: detailperkuliahan.getPerkuliahan() != null
											? detailperkuliahan.getPerkuliahan().getMatakuliah()
											: detailperkuliahan.getMatakuliahKonversi();

							NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
									mahasiswaDapatKelompokPkl.getMahasiswa().getTahunangkatan(),
									mahasiswaDapatKelompokPkl.getMahasiswa().getJurusan(),
									mahasiswaDapatKelompokPkl.getMahasiswa().getJurusan().getFakultas(),
									mahasiswaDapatKelompokPkl.getKelompokPkl().getPkl().getTahunAkademik(),
									mahasiswaDapatKelompokPkl.getKelompokPkl().getPkl().getSemester(),
									matakuliah == null ? "" : matakuliah.getKode(),
									matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

							if (nilaiHuruf != null) {
								mahasiswaDapatKelompokPkl.setTotalIP(nilaiHuruf.getNilaiDiIPK());
								mahasiswaDapatKelompokPkl.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
								mahasiswaDapatKelompokPkl.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
							}
							Common.refreshUpdate(mahasiswaDapatKelompokPkl);

							try {
								if (mahasiswaDapatKelompokPkl.getDetailperkuliahan() != null) {

									Session session = HibernateUtil.currentSession();
									session.refresh(detailperkuliahan);

									detailperkuliahan.setTotalNilai(mahasiswaDapatKelompokPkl.getTotalNilai());
									detailperkuliahan.setTotalIP(mahasiswaDapatKelompokPkl.getTotalIP());
									detailperkuliahan.setNilaiHuruf(mahasiswaDapatKelompokPkl.getNilaiHuruf());
									detailperkuliahan.setLulus(mahasiswaDapatKelompokPkl.getLulus());

									Double totalSementara = mahasiswaDapatKelompokPkl.getTotalNilai();
									nilaiHuruf = Common.getNilaiHuruf(totalSementara,
											detailperkuliahan.getMahasiswa().getTahunangkatan(),
											detailperkuliahan.getMahasiswa().getJurusan(),
											detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
											detailperkuliahan.getTahunAkademik(),
											detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
													: Perkuliahan.GANJIL,
											matakuliah == null ? "" : matakuliah.getKode(),
											matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

									detailperkuliahan.setTotalNilaiSementara(totalSementara);
									detailperkuliahan.setNilaiHurufSementara(
											nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
									detailperkuliahan
											.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

									Common.refreshUpdate(session, detailperkuliahan);
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianPklHelper.java:366");
							}

							footerTotal.setLabel(mahasiswaDapatKelompokPkl.getTotalNilai() == null ? ""
									: (Common.numberFormat.get().format(mahasiswaDapatKelompokPkl.getTotalNilai()) + " ("
											+ mahasiswaDapatKelompokPkl.getNilaiHuruf() + ")"));
						}
					});
				}
			} else {

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(subRows);
				row.appendChild(new Label(parent.getNama()));
				row.appendChild(new MyLabelAgakKecil(parent.getKeterangan()));
				row.appendChild(new Label(""));
				row.appendChild(new Label(""));

				for (final KomponenPenilaianPkl komponenPenilaianPkl : datas) {

					row = new MyFormRow();
					row.setParent(subRows);
					Hbox hbox = new Hbox();
					row.appendChild(hbox);
					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					hbox.appendChild(new Label(komponenPenilaianPkl.getNama()));
					row.appendChild(new MyLabelAgakKecil(komponenPenilaianPkl.getKeterangan()));
					row.appendChild(new Label(Common.numberFormat.get().format(komponenPenilaianPkl.getBobot())));

					Boolean bolehMenilai = true;

					try {
						bolehMenilai = (Boolean) (kolom == null ? true
								: classMetadata.getPropertyValue(komponenPenilaianPkl, kolom, EntityMode.POJO));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianPklHelper.java:403");

					}

					if (tbmuser.getMahasiswa() != null || !bolehMenilai) {
						row.appendChild(new Label(Common.numberFormat.get()
								.format(mahasiswaDapatKelompokPkl.retreiveDetailNilai(komponenPenilaianPkl))));
					} else {

						final MyDoublebox nilai = new MyDoublebox(
								mahasiswaDapatKelompokPkl.retreiveDetailNilai(komponenPenilaianPkl));
						nilai.setWidth("90%");
						row.appendChild(nilai);

						nilai.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								mahasiswaDapatKelompokPkl.populateDetailNilai(komponenPenilaianPkl, nilai.getValue(),
										true);
								mahasiswaDapatKelompokPkl
										.setTotalNilai(mahasiswaDapatKelompokPkl.hitungTotalNilai(true));
								Double total = mahasiswaDapatKelompokPkl.getTotalNilai();

								Detailperkuliahan detailperkuliahan = mahasiswaDapatKelompokPkl.getDetailperkuliahan();
								Matakuliah matakuliah = detailperkuliahan == null ? null
										: detailperkuliahan.getPerkuliahan() != null
												? detailperkuliahan.getPerkuliahan().getMatakuliah()
												: detailperkuliahan.getMatakuliahKonversi();

								NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
										mahasiswaDapatKelompokPkl.getMahasiswa().getTahunangkatan(),
										mahasiswaDapatKelompokPkl.getMahasiswa().getJurusan(),
										mahasiswaDapatKelompokPkl.getMahasiswa().getJurusan().getFakultas(),
										mahasiswaDapatKelompokPkl.getKelompokPkl().getPkl().getTahunAkademik(),
										mahasiswaDapatKelompokPkl.getKelompokPkl().getPkl().getSemester(),
										matakuliah == null ? "" : matakuliah.getKode(),
										matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

								if (nilaiHuruf != null) {
									mahasiswaDapatKelompokPkl.setTotalIP(nilaiHuruf.getNilaiDiIPK());
									mahasiswaDapatKelompokPkl.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
									mahasiswaDapatKelompokPkl
											.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
								}
								Common.refreshUpdate(mahasiswaDapatKelompokPkl);

								if (mahasiswaDapatKelompokPkl.getDetailperkuliahan() != null) {

									detailperkuliahan.setTotalNilai(mahasiswaDapatKelompokPkl.getTotalNilai());
									detailperkuliahan.setTotalIP(mahasiswaDapatKelompokPkl.getTotalIP());
									detailperkuliahan.setNilaiHuruf(mahasiswaDapatKelompokPkl.getNilaiHuruf());
									detailperkuliahan.setLulus(mahasiswaDapatKelompokPkl.getLulus());

									Double totalSementara = mahasiswaDapatKelompokPkl.getTotalNilai();
									nilaiHuruf = Common.getNilaiHuruf(totalSementara,
											detailperkuliahan.getMahasiswa().getTahunangkatan(),
											detailperkuliahan.getMahasiswa().getJurusan(),
											detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
											detailperkuliahan.getTahunAkademik(),
											detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
													: Perkuliahan.GANJIL,
											matakuliah == null ? "" : matakuliah.getKode(),
											matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

									detailperkuliahan.setTotalNilaiSementara(totalSementara);
									detailperkuliahan.setNilaiHurufSementara(
											nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
									detailperkuliahan
											.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

									Common.refreshUpdate(detailperkuliahan);
								}

								footerTotal.setLabel(mahasiswaDapatKelompokPkl.getTotalNilai() == null ? ""
										: (Common.numberFormat.get().format(mahasiswaDapatKelompokPkl.getTotalNilai()) + " ("
												+ mahasiswaDapatKelompokPkl.getNilaiHuruf() + ")"));
							}
						});
					}
				}

			}
		}

		Foot foot = new Foot();
		subGrid.appendChild(foot);

		Footer footer = new Footer("");
		foot.appendChild(footer);
		footer = new Footer("");
		foot.appendChild(footer);
		footer = new Footer("Total");
		foot.appendChild(footer);
		foot.appendChild(footerTotal);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
				loadData(null);
			}
		});
		cancel.setParent(toolbar);

		addWindow.onModal();
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria crit = session.createCriteria(MahasiswaDapatKelompokPkl.class).add(Restrictions.eq("diterima", true))

				.createAlias("mahasiswa", "mahasiswa")

				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", nim.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nim.getValue().trim(), MatchMode.ANYWHERE)))

				.add(Restrictions.eq("kelompokPkl", kelompokPkl));

		if (order) {
			crit.addOrder(Order.asc("id"));
		}
		return crit;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<MahasiswaDapatKelompokPkl> mahasiswaDapatKelompokPkl = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(mahasiswaDapatKelompokPkl);
		grid.setRowRenderer(new DetailKelompokPklRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display(final KelompokPkl kelompokPkl, final Component component) {
		this.kelompokPkl = kelompokPkl;
		Common.clear(component);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar mahasiswa yang mengikuti KKN"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("id_pkl", kelompokPkl.getId());
				Report.generatePDFReport(Report.PDF, parameters, "penerima_kelompok_pkl",
						ais.ui.util.WaktuUtil.getDate());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Singkronkan Nilai", "/img/Configure.gif");
		button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						List<MahasiswaDapatKelompokPkl> mahasiswaDapatKelompokPkls = session
								.createCriteria(MahasiswaDapatKelompokPkl.class).add(Restrictions.eq("diterima", true))
								.addOrder(Order.asc("id")).add(Restrictions.eq("kelompokPkl", kelompokPkl)).list();
						for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : mahasiswaDapatKelompokPkls) {
							Mahasiswa mahasiswa = mahasiswaDapatKelompokPkl.getMahasiswa();

							Detailperkuliahan detailperkuliahan = mahasiswaDapatKelompokPkl.getDetailperkuliahan();

							if (detailperkuliahan == null) {
								detailperkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
										.add(Restrictions.eq("mahasiswa", mahasiswa))
										.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
										.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
										.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)
										.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
										.add(Restrictions.or(
												Restrictions.ilike("matakuliah.nama", Common.getBahasaConfig("pkl"),
														MatchMode.ANYWHERE),
												Restrictions.ilike("matakuliahKonversi.nama",
														Common.getBahasaConfig("pkl"), MatchMode.ANYWHERE)))
										.addOrder(Order.desc("semester")).setMaxResults(1).uniqueResult();
								mahasiswaDapatKelompokPkl.setDetailperkuliahan(detailperkuliahan);
								Common.refreshUpdate(session, mahasiswaDapatKelompokPkl);
							}

							if (detailperkuliahan != null) {

								session.refresh(detailperkuliahan);

								List<FormatNilai> formatNilais = Common.getFormatNilais(session,
										detailperkuliahan.getPerkuliahan());
								for (FormatNilai formatNilai : formatNilais) {
									detailperkuliahan.populateDetailNilai(formatNilai, null,
											mahasiswaDapatKelompokPkl.getTotalNilai(), true, tbmuser);
								}

								detailperkuliahan.setTotalNilai(mahasiswaDapatKelompokPkl.getTotalNilai());
								detailperkuliahan.setTotalIP(mahasiswaDapatKelompokPkl.getTotalIP());
								detailperkuliahan.setNilaiHuruf(mahasiswaDapatKelompokPkl.getNilaiHuruf());
								detailperkuliahan.setLulus(mahasiswaDapatKelompokPkl.getLulus());

								Matakuliah matakuliah = detailperkuliahan == null ? null
										: detailperkuliahan.getPerkuliahan() != null
												? detailperkuliahan.getPerkuliahan().getMatakuliah()
												: detailperkuliahan.getMatakuliahKonversi();

								Double totalSementara = mahasiswaDapatKelompokPkl.getTotalNilai();
								NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(totalSementara,
										detailperkuliahan.getMahasiswa().getTahunangkatan(),
										detailperkuliahan.getMahasiswa().getJurusan(),
										detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
										detailperkuliahan.getTahunAkademik(),
										detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
												: Perkuliahan.GANJIL,
										matakuliah == null ? "" : matakuliah.getKode(),
										matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

								detailperkuliahan.setTotalNilaiSementara(totalSementara);
								detailperkuliahan
										.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
								detailperkuliahan
										.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

								Common.refreshUpdate(session, detailperkuliahan);

							}
						}
					}
				});
			}

		});
		button.setParent(toolbar);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs : ")));
		toolbar.appendChild(nim = new Textbox());
		nim.setWidth("");
		nim.setWidth("70px");
		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Matakuliah Pkl");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total Nilai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai Huruf");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("8%");

		loadData(null);

		paging.setParent(groupbox);
	}

}
