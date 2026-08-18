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
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kkn.KknPunyaKomponenPenilaianKkn;
import ais.database.model.kkn.KomponenPenilaianKkn;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PenilaianKknHelper implements DataLoader {

	private MyGrid grid;
	private KelompokKkn kelompokKkn;
	private Paging paging;
	private Textbox nim;
	private Tbmuser tbmuser;

	class DetailKelompokKknRenderer extends ais.ui.util.MyRowRenderer {

		Tbmuser tbmuser = Common.getCurrentUser();

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn = (MahasiswaDapatKelompokKkn) data;

			Mahasiswa mahasiswa = mahasiswaDapatKelompokKkn.getMahasiswa();

			Detailperkuliahan detailperkuliahan = mahasiswaDapatKelompokKkn.getDetailperkuliahan();

			if (detailperkuliahan == null) {
				detailperkuliahan = (Detailperkuliahan) HibernateUtil.currentSession()
						.createCriteria(Detailperkuliahan.class).add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
						.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)
						.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
						.add(Restrictions.or(
								Restrictions.ilike("matakuliah.nama", Common.getBahasaConfig("kkn"),
										MatchMode.ANYWHERE),
								Restrictions.ilike("matakuliahKonversi.nama", Common.getBahasaConfig("kkn"),
										MatchMode.ANYWHERE)))
						.addOrder(Order.desc("semester")).setMaxResults(1).uniqueResult();
				mahasiswaDapatKelompokKkn.setDetailperkuliahan(detailperkuliahan);
				Common.refreshUpdate(mahasiswaDapatKelompokKkn);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(vbox);

			RevisiHelper.createNewRevisi(MahasiswaDapatKelompokKkn.class, mahasiswaDapatKelompokKkn, mahasiswa.getNim())
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
						mahasiswaDapatKelompokKkn.setDetailperkuliahan(
								(Detailperkuliahan) ambilDataMatakuliahBanbox.getAttribute("detailperkuliahan"));
						Common.refreshUpdate(mahasiswaDapatKelompokKkn);
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

				new Label(mahasiswaDapatKelompokKkn.getTotalNilai() == null ? ""
						: Common.numberFormat.get().format(mahasiswaDapatKelompokKkn.getTotalNilai())).setParent(row);

				new Label(mahasiswaDapatKelompokKkn.getNilaiHuruf()).setParent(row);

				Hbox toolbar = new Hbox();
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Penilaian", "/img/svg/edit-box-line.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						init(mahasiswaDapatKelompokKkn);

					}

				});
				button.setParent(toolbar);
				toolbar.setParent(row);
			}

		}

	}

	@SuppressWarnings({ "unchecked" })
	private void init(final MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn) throws Exception {
		final MyWindow addWindow = new MyWindow();
		addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Tbmuser tbmuser = Common.getCurrentUser();

		addWindow.setTitle("Penilaian Kkn");
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

				TreeMap<String, Dosen> dosens = kelompokKkn.populateDosen();
				for (String key : dosens.keySet()) {
					try {
						Dosen d = dosens.get(key);
						if (d.getId().equals(tbmuser.getDosen().getId())) {
							kolom = key;
							break;
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianKknHelper.java:237");
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianKknHelper.java:243");
		}

		final Footer footerTotal = new Footer(mahasiswaDapatKelompokKkn.getTotalNilai() == null ? ""
				: (Common.numberFormat.get().format(mahasiswaDapatKelompokKkn.getTotalNilai()) + " ("
						+ mahasiswaDapatKelompokKkn.getNilaiHuruf() + ")"));

		Session session = HibernateUtil.currentSession();
		List<KomponenPenilaianKkn> kknPunyaKomponenPenilaianKkns = session
				.createCriteria(KknPunyaKomponenPenilaianKkn.class)
				.setProjection(Projections.groupProperty("komponenPenilaianKkn"))
				.createAlias("komponenPenilaianKkn", "komponenPenilaianKkn")
				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianKkn.aktif"),
						Restrictions.eq("komponenPenilaianKkn.aktif", true)))
				.add(Restrictions.eq("kkn", kelompokKkn.getKkn())).list();
		TreeMap<KomponenPenilaianKkn, List<KomponenPenilaianKkn>> dataKomponenPenilaian = new TreeMap<KomponenPenilaianKkn, List<KomponenPenilaianKkn>>();
		for (KomponenPenilaianKkn komponenPenilaianKkn : kknPunyaKomponenPenilaianKkns) {
			if (komponenPenilaianKkn.getParent() != null) {
				if (!dataKomponenPenilaian.keySet().contains(komponenPenilaianKkn.getParent())) {
					List<KomponenPenilaianKkn> datas = new ArrayList<KomponenPenilaianKkn>();
					datas.add(komponenPenilaianKkn);
					dataKomponenPenilaian.put(komponenPenilaianKkn.getParent(), datas);
				} else {
					dataKomponenPenilaian.get(komponenPenilaianKkn.getParent()).add(komponenPenilaianKkn);
				}
			}
		}

		for (KomponenPenilaianKkn komponenPenilaianKkn : kknPunyaKomponenPenilaianKkns) {
			if (komponenPenilaianKkn.getParent() == null && !dataKomponenPenilaian.containsKey(komponenPenilaianKkn)) {
				List<KomponenPenilaianKkn> datas = new ArrayList<KomponenPenilaianKkn>();
				dataKomponenPenilaian.put(komponenPenilaianKkn, datas);
			}
		}

		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(KomponenPenilaianKkn.class);

		for (final KomponenPenilaianKkn parent : dataKomponenPenilaian.keySet()) {

			final List<KomponenPenilaianKkn> datas = dataKomponenPenilaian.get(parent);
			if (datas.isEmpty()) {

				Row row = new Row();
				row.setValign("top");
				row.setParent(subRows);
				row.appendChild(new Label(parent.getNama()));
				row.appendChild(new MyLabelAgakKecil(parent.getKeterangan()));
				row.appendChild(new Label(Common.numberFormat.get().format(parent.getBobot())));

				Boolean bolehMenilai = true;

				try {
					bolehMenilai = (Boolean) (kolom == null || parent == null || classMetadata == null
							|| !java.util.Arrays.asList(classMetadata.getPropertyNames()).contains(kolom) ? true
									: classMetadata.getPropertyValue(parent, kolom, EntityMode.POJO));
					if (bolehMenilai == null) {
						bolehMenilai = true;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianKknHelper.java:297");

				}

				if (tbmuser.getMahasiswa() != null || !bolehMenilai) {
					row.appendChild(new Label(
							Common.numberFormat.get().format(mahasiswaDapatKelompokKkn.retreiveDetailNilai(parent))));
				} else {
					final MyDoublebox nilai = new MyDoublebox(mahasiswaDapatKelompokKkn.retreiveDetailNilai(parent));
					nilai.setWidth("90%");
					row.appendChild(nilai);

					nilai.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							mahasiswaDapatKelompokKkn.populateDetailNilai(parent, nilai.getValue(), true);
							mahasiswaDapatKelompokKkn.setTotalNilai(mahasiswaDapatKelompokKkn.hitungTotalNilai(true));

							Detailperkuliahan detailperkuliahan = mahasiswaDapatKelompokKkn.getDetailperkuliahan();
							Matakuliah matakuliah = detailperkuliahan == null ? null
									: detailperkuliahan.getPerkuliahan() != null
											? detailperkuliahan.getPerkuliahan().getMatakuliah()
											: detailperkuliahan.getMatakuliahKonversi();

							Double total = mahasiswaDapatKelompokKkn.getTotalNilai();
							NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
									mahasiswaDapatKelompokKkn.getMahasiswa().getTahunangkatan(),
									mahasiswaDapatKelompokKkn.getMahasiswa().getJurusan(),
									mahasiswaDapatKelompokKkn.getMahasiswa().getJurusan().getFakultas(),
									mahasiswaDapatKelompokKkn.getKelompokKkn().getKkn().getTahunAkademik(),
									mahasiswaDapatKelompokKkn.getKelompokKkn().getKkn().getSemester(),
									matakuliah == null ? "" : matakuliah.getKode(),
									matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

							if (nilaiHuruf != null) {
								mahasiswaDapatKelompokKkn.setTotalIP(nilaiHuruf.getNilaiDiIPK());
								mahasiswaDapatKelompokKkn.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
								mahasiswaDapatKelompokKkn.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
							}
							Common.refreshUpdate(mahasiswaDapatKelompokKkn);

							if (mahasiswaDapatKelompokKkn.getDetailperkuliahan() != null) {
								detailperkuliahan.setTotalNilai(mahasiswaDapatKelompokKkn.getTotalNilai());
								detailperkuliahan.setTotalIP(mahasiswaDapatKelompokKkn.getTotalIP());
								detailperkuliahan.setNilaiHuruf(mahasiswaDapatKelompokKkn.getNilaiHuruf());
								detailperkuliahan.setLulus(mahasiswaDapatKelompokKkn.getLulus());

								Double totalSementara = mahasiswaDapatKelompokKkn.getTotalNilai();
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
								detailperkuliahan
										.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
								detailperkuliahan
										.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

								Common.refreshUpdate(detailperkuliahan);
							}

							footerTotal.setLabel(mahasiswaDapatKelompokKkn.getTotalNilai() == null ? ""
									: (Common.numberFormat.get().format(mahasiswaDapatKelompokKkn.getTotalNilai()) + " ("
											+ mahasiswaDapatKelompokKkn.getNilaiHuruf() + ")"));
						}
					});
				}
			} else {

				Row row = new Row();
				row.setValign("top");
				row.setParent(subRows);
				row.appendChild(new Label(parent.getNama()));
				row.appendChild(new MyLabelAgakKecil(parent.getKeterangan()));
				row.appendChild(new Label(""));
				row.appendChild(new Label(""));

				for (final KomponenPenilaianKkn komponenPenilaianKkn : datas) {

					row = new Row();
					row.setParent(subRows);
					Hbox hbox = new Hbox();
					row.appendChild(hbox);
					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					hbox.appendChild(new Label(komponenPenilaianKkn.getNama()));
					row.appendChild(new MyLabelAgakKecil(komponenPenilaianKkn.getKeterangan()));
					row.appendChild(new Label(Common.numberFormat.get().format(komponenPenilaianKkn.getBobot())));

					Boolean bolehMenilai = true;

					try {
						bolehMenilai = (Boolean) (kolom == null || komponenPenilaianKkn == null || classMetadata == null
								|| !java.util.Arrays.asList(classMetadata.getPropertyNames()).contains(kolom) ? true
										: classMetadata.getPropertyValue(komponenPenilaianKkn, kolom, EntityMode.POJO));
						if (bolehMenilai == null) {
							bolehMenilai = true;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianKknHelper.java:399");

					}

					if (tbmuser.getMahasiswa() != null || !bolehMenilai) {
						row.appendChild(new Label(Common.numberFormat.get()
								.format(mahasiswaDapatKelompokKkn.retreiveDetailNilai(komponenPenilaianKkn))));
					} else {

						final MyDoublebox nilai = new MyDoublebox(
								mahasiswaDapatKelompokKkn.retreiveDetailNilai(komponenPenilaianKkn));
						nilai.setWidth("90%");
						row.appendChild(nilai);

						nilai.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								mahasiswaDapatKelompokKkn.populateDetailNilai(komponenPenilaianKkn, nilai.getValue(),
										true);
								mahasiswaDapatKelompokKkn
										.setTotalNilai(mahasiswaDapatKelompokKkn.hitungTotalNilai(true));
								Double total = mahasiswaDapatKelompokKkn.getTotalNilai();

								Detailperkuliahan detailperkuliahan = mahasiswaDapatKelompokKkn.getDetailperkuliahan();
								Matakuliah matakuliah = detailperkuliahan == null ? null
										: detailperkuliahan.getPerkuliahan() != null
												? detailperkuliahan.getPerkuliahan().getMatakuliah()
												: detailperkuliahan.getMatakuliahKonversi();

								NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
										mahasiswaDapatKelompokKkn.getMahasiswa().getTahunangkatan(),
										mahasiswaDapatKelompokKkn.getMahasiswa().getJurusan(),
										mahasiswaDapatKelompokKkn.getMahasiswa().getJurusan().getFakultas(),
										mahasiswaDapatKelompokKkn.getKelompokKkn().getKkn().getTahunAkademik(),
										mahasiswaDapatKelompokKkn.getKelompokKkn().getKkn().getSemester(),
										matakuliah == null ? "" : matakuliah.getKode(),
										matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

								if (nilaiHuruf != null) {
									mahasiswaDapatKelompokKkn.setTotalIP(nilaiHuruf.getNilaiDiIPK());
									mahasiswaDapatKelompokKkn.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
									mahasiswaDapatKelompokKkn
											.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
								}
								Common.refreshUpdate(mahasiswaDapatKelompokKkn);

								try {
									if (mahasiswaDapatKelompokKkn.getDetailperkuliahan() != null) {

										Session session = HibernateUtil.currentSession();
										session.refresh(detailperkuliahan);

										detailperkuliahan.setTotalNilai(mahasiswaDapatKelompokKkn.getTotalNilai());
										detailperkuliahan.setTotalIP(mahasiswaDapatKelompokKkn.getTotalIP());
										detailperkuliahan.setNilaiHuruf(mahasiswaDapatKelompokKkn.getNilaiHuruf());
										detailperkuliahan.setLulus(mahasiswaDapatKelompokKkn.getLulus());

										Double totalSementara = mahasiswaDapatKelompokKkn.getTotalNilai();
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
										detailperkuliahan.setTotalIPSementara(
												nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

										Common.refreshUpdate(session, detailperkuliahan);
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianKknHelper.java:478");
								}

								footerTotal.setLabel(mahasiswaDapatKelompokKkn.getTotalNilai() == null ? ""
										: (Common.numberFormat.get().format(mahasiswaDapatKelompokKkn.getTotalNilai()) + " ("
												+ mahasiswaDapatKelompokKkn.getNilaiHuruf() + ")"));
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
		Criteria crit = session.createCriteria(MahasiswaDapatKelompokKkn.class).add(Restrictions.eq("diterima", true))

				.createAlias("mahasiswa", "mahasiswa")

				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", nim.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nim.getValue().trim(), MatchMode.ANYWHERE)))

				.add(Restrictions.eq("kelompokKkn", kelompokKkn));

		if (order) {
			crit.addOrder(Order.asc("id"));
		}
		return crit;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<MahasiswaDapatKelompokKkn> mahasiswaDapatKelompokKkn = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(mahasiswaDapatKelompokKkn);
		grid.setRowRenderer(new DetailKelompokKknRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display(final KelompokKkn kelompokKkn, final Component component) {
		this.kelompokKkn = kelompokKkn;
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
				parameters.put("id_kkn", kelompokKkn.getId());
				Report.generatePDFReport(Report.PDF, parameters, "penerima_kelompok_kkn",
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
						List<MahasiswaDapatKelompokKkn> mahasiswaDapatKelompokKkns = session
								.createCriteria(MahasiswaDapatKelompokKkn.class).add(Restrictions.eq("diterima", true))
								.addOrder(Order.asc("id")).add(Restrictions.eq("kelompokKkn", kelompokKkn)).list();
						for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : mahasiswaDapatKelompokKkns) {
							Mahasiswa mahasiswa = mahasiswaDapatKelompokKkn.getMahasiswa();

							Detailperkuliahan detailperkuliahan = mahasiswaDapatKelompokKkn.getDetailperkuliahan();

							if (detailperkuliahan == null) {
								detailperkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
										.add(Restrictions.eq("mahasiswa", mahasiswa))
										.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
										.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
										.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)
										.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
										.add(Restrictions.or(
												Restrictions.ilike("matakuliah.nama", Common.getBahasaConfig("kkn"),
														MatchMode.ANYWHERE),
												Restrictions.ilike("matakuliahKonversi.nama",
														Common.getBahasaConfig("kkn"), MatchMode.ANYWHERE)))
										.addOrder(Order.desc("semester")).setMaxResults(1).uniqueResult();
								mahasiswaDapatKelompokKkn.setDetailperkuliahan(detailperkuliahan);
								Common.refreshUpdate(session, mahasiswaDapatKelompokKkn);
							}

							if (detailperkuliahan != null) {

								session.refresh(detailperkuliahan);

								List<FormatNilai> formatNilais = Common.getFormatNilais(session,
										detailperkuliahan.getPerkuliahan());
								for (FormatNilai formatNilai : formatNilais) {
									detailperkuliahan.populateDetailNilai(formatNilai, null,
											mahasiswaDapatKelompokKkn.getTotalNilai(), true, tbmuser);
								}

								detailperkuliahan.setTotalNilai(mahasiswaDapatKelompokKkn.getTotalNilai());
								detailperkuliahan.setTotalIP(mahasiswaDapatKelompokKkn.getTotalIP());
								detailperkuliahan.setNilaiHuruf(mahasiswaDapatKelompokKkn.getNilaiHuruf());
								detailperkuliahan.setLulus(mahasiswaDapatKelompokKkn.getLulus());

								Matakuliah matakuliah = detailperkuliahan == null ? null
										: detailperkuliahan.getPerkuliahan() != null
												? detailperkuliahan.getPerkuliahan().getMatakuliah()
												: detailperkuliahan.getMatakuliahKonversi();

								Double totalSementara = detailperkuliahan.hitungTotalNilaiSementara(true, formatNilais);
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
		column.setLabel("Matakuliah Kkn");
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
