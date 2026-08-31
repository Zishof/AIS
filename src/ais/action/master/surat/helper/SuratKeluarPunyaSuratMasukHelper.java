package ais.action.master.surat.helper;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;
import ais.database.model.surat.OpsiSuratMasukValue;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI ZK modul persuratan untuk mengaitkan {@link SuratMasuk} rujukan (lampiran) ke satu
 * {@link SuratKeluar}. Berbeda dari kebanyakan helper "punya" lainnya yang memakai tabel relasi
 * terpisah, relasi ini disimpan sebagai daftar id surat masuk berformat CSV pada kolom tunggal
 * {@code suratKeluar.getSuratMasuks()}. Dipasang pada panel detail satu surat keluar, menampilkan
 * daftar surat masuk terkait sebagai grid dengan ringkasan status alur persetujuan, opsi, dan
 * lampiran gambar tiap surat masuk.
 *
 * <p>
 * Tombol "Tambah Lampiran" membuka dialog {@code AmbilDataSuratMasukBanyak} (pemilih surat masuk
 * multi-pilih, disaring per {@code tipe}) dan menambahkan id surat masuk yang dipilih ke string
 * CSV {@code suratMasuks}. Setiap baris menampilkan riwayat status alur persetujuan surat masuk
 * (disusun sebagai daftar HTML {@code <li>}, membedakan alur berbasis jenis jabatan vs jabatan
 * spesifik, serta status "sudah ditindaklanjuti"/"ditolak"/"belum ditindaklanjuti" dengan warna
 * berbeda), daftar opsi surat masuk terpilih, dan tombol unduh untuk tiap lampiran gambar surat
 * masuk (mengarahkan ke Google Drive atau menyajikan unduhan langsung sesuai tempat penyimpanan).
 * Tombol hapus per baris meminta konfirmasi lalu menghapus id surat masuk tersebut dari string
 * CSV {@code suratMasuks} (bukan menghapus data {@link SuratMasuk} itu sendiri).
 * </p>
 */
public class SuratKeluarPunyaSuratMasukHelper {

	private MyGrid gridPengarang;
	private boolean delete = false;
	private SuratKeluar suratKeluar;

	/** Membangun helper terikat pada {@code gridPengarang} dan menghitung hak hapus pengguna saat ini. */
	public SuratKeluarPunyaSuratMasukHelper(MyGrid gridPengarang) {
		this.gridPengarang = gridPengarang;
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun panel (border layout) berisi toolbar "Tambah Lampiran" dan grid daftar surat
	 * masuk terkait untuk {@code suratKeluar}, lalu memuat data surat masuk yang sudah terkait.
	 *
	 * @param suratKeluar surat keluar yang detail rujukan surat masuknya ditampilkan/dikelola
	 * @param tampilEdit  bila {@code true}, toolbar tambah ditampilkan; bila {@code false}, hanya grid (mode lihat saja)
	 * @param tipe        tipe surat masuk yang menjadi filter pada dialog pemilih surat masuk
	 * @return border layout siap disisipkan sebagai konten panel detail
	 */
	public Borderlayout initDetail(final SuratKeluar suratKeluar, boolean tampilEdit, final String tipe)
			throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		this.suratKeluar = suratKeluar;
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setVisible(tampilEdit);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig fileupload = new MyToolbarbuttonConfig("Tambah Lampiran Surat Keluar ",
				"/img/File-Upload-icon.png");
		fileupload.setParent(toolbar);
		fileupload.setTooltiptext("Tambah");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final AmbilDataSuratMasukBanyak ambilDataSuratMasukBanyak = new AmbilDataSuratMasukBanyak(
						new ArrayList<SuratMasuk>(), tipe);
				ambilDataSuratMasukBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataSuratMasukBanyak.setHeight("95%");
				ambilDataSuratMasukBanyak.setWidth("700px");

				ambilDataSuratMasukBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						@SuppressWarnings("unchecked")
						List<SuratMasuk> suratMasukes = (List<SuratMasuk>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						if (suratKeluar.getId() != null)
							session.refresh(suratKeluar);
						String s = suratKeluar.getSuratMasuks();
						for (SuratMasuk suratMasuk : suratMasukes) {
							s += s.trim().isEmpty() ? suratMasuk.getId() + "" : "," + suratMasuk.getId();
						}

						suratKeluar.setSuratMasuks(s);

						if (suratKeluar.getId() != null)
							Common.refreshUpdate(session, suratKeluar);

						ambilDataSuratMasukBanyak.detach();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDetail(suratKeluar);
							}
						});

					}
				});

				ambilDataSuratMasukBanyak.onModal();

			}
		};
		fileupload.addEventListener("onClick", eventListener);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridPengarang);
		gridPengarang.setParent(center);
		gridPengarang.setWidth("100%");
		gridPengarang.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridPengarang);

		MyColumnConfig column = new MyColumnConfig("Lampiran");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(suratKeluar);

		return borderlayout;
	}

	/** Memuat baris-baris {@link SuratMasuk} yang id-nya tercantum pada CSV {@code suratKeluar.getSuratMasuks()} dan merendernya ke grid. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(SuratKeluar suratKeluar) throws Exception {

		String inSql = "";
		for (String id : suratKeluar.getSuratMasuks().split(",")) {
			try {
				if (!id.trim().isEmpty()) {
					inSql += inSql.isEmpty() ? id : "," + id;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/SuratKeluarPunyaSuratMasukHelper.java:152");
				// TODO: handle exception
			}
		}

		Session session = HibernateUtil.currentSession();
		List<SuratMasuk> suratMasuks = session.createCriteria(SuratMasuk.class)
				.add(inSql.trim().isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.sqlRestriction("this_.id in (" + inSql + ")"))
				.addOrder(Order.asc("id")).list();

		Rows rows = gridPengarang.getRows() == null ? new Rows() : gridPengarang.getRows();
		rows.setParent(gridPengarang);

		for (SuratMasuk suratMasuk : suratMasuks) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, suratMasuk);
		}
		StreamingHibernateUtil.getInstance().closeSession();
	}

	/**
	 * Mengisi {@code rowUtamaLagi} dengan ringkasan {@code suratMasuk} (nama, tanggal,
	 * klasifikasi, kode, perihal, catatan revisi/penolakan bila ada), daftar riwayat status alur
	 * persetujuan dan opsi terpilih, tombol unduh untuk tiap lampiran gambar surat masuk, dan
	 * tombol hapus (bila pengguna berhak) yang meminta konfirmasi sebelum melepas id surat masuk
	 * ini dari CSV {@code suratKeluar.getSuratMasuks()}.
	 */
	@SuppressWarnings("unchecked")
	public void initRow(final Row rowUtamaLagi, final SuratMasuk suratMasuk) throws Exception {
		rowUtamaLagi.setValign("top");
		rowUtamaLagi.setAttribute("suratMasuk", suratMasuk);

		Vbox vbox1 = new Vbox();
		vbox1.setParent(rowUtamaLagi);

		Vbox a;
		(a = new Vbox()).setParent(vbox1);
		Vbox vbox = new Vbox();
		a.appendChild(vbox);
		vbox.appendChild(new MyLabelBoldAja(
				suratMasuk.getNama() + " (tgl " + Common.dateFormat6.get().format(suratMasuk.getTanggalSurat()) + ")"));

		vbox.appendChild(new MyLabelAgakKecil(
				suratMasuk.getKlasifikasiSuratMasuk() == null ? "" : suratMasuk.getKlasifikasiSuratMasuk().getNama()));
		vbox.appendChild(new MyLabelAgakKecil(suratMasuk.getKode()));
		vbox.appendChild(new MyLabelAgakKecil(suratMasuk.getPerihal()));

		if (suratMasuk.getAlurDitolak() != null && suratMasuk.getAlurDitolak().getTelahDirevisi()) {
			try {
				vbox.appendChild(new MyLabelAgakKecilBoldMerah(
						"Direvisi dengan catatan : " + suratMasuk.getAlurDitolak().getCatatanRevisi()));
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/SuratKeluarPunyaSuratMasukHelper.java:200");
			}
			try {
				vbox.appendChild(new MyLabelAgakKecilBoldMerah(
						"Sebelumnya ditolak dengan catatan : " + suratMasuk.getAlurDitolak().getKeterangan()));
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/SuratKeluarPunyaSuratMasukHelper.java:206");
			}
		} else if (suratMasuk.getAlurDitolak() != null && suratMasuk.getAlurDitolak().getDitolak()) {
			try {
				vbox.appendChild(new MyLabelAgakKecilBoldMerah(
						"Ditolak dengan catatan : " + suratMasuk.getAlurDitolak().getKeterangan()));
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/SuratKeluarPunyaSuratMasukHelper.java:213");
			}
		}

		String html = "";
		Session session = HibernateUtil.currentSession();
		List<AlurPersetujuanSuratMasukStatus> alurPersetujuanSuratMasukStatuss = session
				.createCriteria(AlurPersetujuanSuratMasukStatus.class).add(Restrictions.isNotNull("kodeUnik"))
				.add(Restrictions.eq("suratMasuk", suratMasuk)).addOrder(Order.asc("id")).list();
		for (AlurPersetujuanSuratMasukStatus myAlurPersetujuanSuratMasukStatus : alurPersetujuanSuratMasukStatuss) {
			if (myAlurPersetujuanSuratMasukStatus.getJenisJabatan() == null
					&& myAlurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk() != null
					&& myAlurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk().getJenisJabatan() != null) {
				html += "<li>" + myAlurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk() + " : "
						+ (myAlurPersetujuanSuratMasukStatus.getDisetujui()
								? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;\">Sudah ditindak-lanjuti "
										+ (myAlurPersetujuanSuratMasukStatus.getPejabat() == null
												|| myAlurPersetujuanSuratMasukStatus.getPejabat().getPegawai() == null
														? (myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getDosen() == null
																		? ""
																		: " " + myAlurPersetujuanSuratMasukStatus
																				.getPejabat().getDosen().getNama())
														: " " + myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getPegawai().getNama())
										+ (myAlurPersetujuanSuratMasukStatus.getWaktuPersetujuan() == null ? ""
												: " pada waktu " + Common.dateFormat3.get().format(
														myAlurPersetujuanSuratMasukStatus.getWaktuPersetujuan()))
										+ "</font>")
								: myAlurPersetujuanSuratMasukStatus.getDitolak()
										? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Ditolak "
												+ (myAlurPersetujuanSuratMasukStatus.getPejabat() == null
														|| myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getPegawai() == null
																		? (myAlurPersetujuanSuratMasukStatus
																				.getPejabat().getDosen() == null
																						? ""
																						: " " + myAlurPersetujuanSuratMasukStatus
																								.getPejabat().getDosen()
																								.getNama())
																		: " " + myAlurPersetujuanSuratMasukStatus
																				.getPejabat().getPegawai().getNama())
												+ (myAlurPersetujuanSuratMasukStatus.getWaktuDitolak() == null ? ""
														: " pada waktu " + Common.dateFormat3.get().format(
																myAlurPersetujuanSuratMasukStatus.getWaktuDitolak()))
												+ "</font>")
										: "<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Belum ditindak lanjuti"
												+ (myAlurPersetujuanSuratMasukStatus.getPejabat() == null ? ""
														: " " + myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getNama())
												+ "</font>")
						+ "</li>";
			} else if (myAlurPersetujuanSuratMasukStatus.getJenisJabatan() != null) {
				html += "<li>" + myAlurPersetujuanSuratMasukStatus.getJenisJabatan().getNama() + " : "
						+ (myAlurPersetujuanSuratMasukStatus.getDisetujui()
								? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;\">Sudah ditindak-lanjuti "
										+ (myAlurPersetujuanSuratMasukStatus.getPejabat() == null
												|| myAlurPersetujuanSuratMasukStatus.getPejabat().getPegawai() == null
														? (myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getDosen() == null
																		? ""
																		: " " + myAlurPersetujuanSuratMasukStatus
																				.getPejabat().getDosen().getNama())
														: " " + myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getPegawai().getNama())
										+ (myAlurPersetujuanSuratMasukStatus.getWaktuPersetujuan() == null ? ""
												: " pada waktu " + Common.dateFormat3.get().format(
														myAlurPersetujuanSuratMasukStatus.getWaktuPersetujuan()))
										+ "</font>")
								: myAlurPersetujuanSuratMasukStatus.getDitolak()
										? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Ditolak "
												+ (myAlurPersetujuanSuratMasukStatus.getPejabat() == null
														|| myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getPegawai() == null
																		? (myAlurPersetujuanSuratMasukStatus
																				.getPejabat().getDosen() == null
																						? ""
																						: " " + myAlurPersetujuanSuratMasukStatus
																								.getPejabat().getDosen()
																								.getNama())
																		: " " + myAlurPersetujuanSuratMasukStatus
																				.getPejabat().getPegawai().getNama())
												+ (myAlurPersetujuanSuratMasukStatus.getWaktuDitolak() == null ? ""
														: " pada waktu " + Common.dateFormat3.get().format(
																myAlurPersetujuanSuratMasukStatus.getWaktuDitolak()))
												+ "</font>")
										: "<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Belum ditindak lanjuti"
												+ (myAlurPersetujuanSuratMasukStatus.getPejabat() == null ? ""
														: " " + myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getNama())
												+ "</font>")
						+ "</li>";
			}
		}

		new ais.ui.util.MyHtml("<font style=\"font-size: xx-small;\"><ul>" + html + "</ul></font>").setParent(vbox);

		html = "";
		List<String> suratMasukValues = session.createCriteria(OpsiSuratMasukValue.class)
				.setProjection(Projections.groupProperty("nama")).add(Restrictions.eq("suratMasuk", suratMasuk)).list();
		for (String opsiSuratMasukValue : suratMasukValues) {
			html += "<li>" + opsiSuratMasukValue + "</li>";
		}

		new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">" + Common.getBahasaConfig("Opsi") + ":<ul>" + html
				+ "</ul></font>").setParent(vbox);

		Hbox hbox = new Hbox();
		hbox.setParent(vbox1);

		Session sessions = StreamingHibernateUtil.getInstance().currentSession();
		List<Object[]> fotoGambarSuratMasuks = suratMasuk == null || suratMasuk.getId() == null
				? new ArrayList<Object[]>()
				: sessions.createCriteria(FotoGambarSuratMasuk.class)
						.setProjection(Projections.projectionList().add(Projections.property("id"))
								.add(Projections.property("nama")))
						.add(Restrictions.eq("suratMasuk", suratMasuk.getId())).addOrder(Order.desc("id")).list();

		for (Object[] fotoGambarSuratMasuk : fotoGambarSuratMasuks) {
			try {
				final Long id = (Long) fotoGambarSuratMasuk[0];
				String nama = (String) fotoGambarSuratMasuk[1];

				Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig(nama, "/img/svg/download.svg");
				button.setTooltiptext("Download " + nama);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Session sessions = StreamingHibernateUtil.getInstance().currentSession();

						FotoGambarSuratMasuk fotoGambarSuratMasuk = (FotoGambarSuratMasuk) sessions
								.createCriteria(FotoGambarSuratMasuk.class).add(Restrictions.idEq(id)).uniqueResult();

						if (fotoGambarSuratMasuk.getGdrive() != null && !fotoGambarSuratMasuk.getGdrive().isEmpty()) {
							ExecutionsCtrl.getCurrent().sendRedirect(fotoGambarSuratMasuk.downloadGDriveUrl(),
									"_blank");
						} else if (fotoGambarSuratMasuk != null) {

							Common.display(fotoGambarSuratMasuk);

						}

						sessions.disconnect();
						sessions.close();
						StreamingHibernateUtil.getInstance().closeSession();

					}

				});
				button.setParent(hbox);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/SuratKeluarPunyaSuratMasukHelper.java:364");
				// TODO: handle exception
			}
		}
		sessions.disconnect();
		sessions.close();
		StreamingHibernateUtil.getInstance().closeSession();

		hbox = new Hbox();
		hbox.setParent(rowUtamaLagi);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (suratKeluar.getId() != null) {

										Session session = HibernateUtil.currentSession();
										session.refresh(suratKeluar);
										String s = suratKeluar.getSuratMasuks();

										s = StringUtils.replace(s, "," + suratKeluar.getId() + ",", "");

										suratKeluar.setSuratMasuks(s);
										Common.refreshUpdate(session, suratKeluar);

									}
									rowUtamaLagi.setVisible(false);
									rowUtamaLagi.detach();
								}

							}
						});

			}
		});
	}

}
