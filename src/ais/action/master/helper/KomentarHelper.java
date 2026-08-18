package ais.action.master.helper;

import java.io.File;
import java.util.List;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.dao.KomentarDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Komentar;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KomentarHelper {

	private Dosen dosen;
	private Mahasiswa mahasiswa;
	private Textbox komentar;
	private String tahunAkademik;
	private Integer semester;
	private Integer semesterPendek;
	private Dosen dosenpa;
	private Integer tahapan;
	private boolean remedial;

	public KomentarHelper(Mahasiswa mahasiswa, String tahunAkademik, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean remedial, Dosen dosenpa) {
		try {
			dosen = Common.getCurrentUser().getDosen();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KomentarHelper.java:56");

		}
		this.dosenpa = dosenpa;
		this.mahasiswa = mahasiswa;
		this.tahunAkademik = tahunAkademik;
		this.semester = semester;
		this.tahapan = tahapan;
		this.semesterPendek = semesterPendek;
		this.remedial = remedial;
	}

	public void display(final EventListener eventListener) throws Exception {
		final MyWindow window = new MyWindow("Masukkan komentar Anda", "normal", false);
		window.setHeight("250px");
		window.setWidth("850px");

		Component component = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
		window.setParent(component);

		window.setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Isi Komentar"));
		komentar = new Textbox();
		row.appendChild(komentar);
		row.setValign("top");
		komentar.setWidth("90%");
		komentar.setRows(5);

		Common.initKeterangan(rows,
				"Komentar ini akan tampil di halaman KRS Mahasiswa, baik di halaman mahasiswa yang bersangkutan maupun dosen pembimbing akademik");

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (komentar.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Mohon maaf, komentar belum diisi. Langkah yang dapat dilakukan: (1) ketik komentar pada kolom yang tersedia; (2) pastikan komentar tidak kosong; (3) klik kirim untuk menyimpan komentar. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				final Tbmuser tbmuser = Common.getCurrentUser();

				// Lakukan Penyimpanan disini
				final Komentar komentar = new Komentar();
				komentar.setDetailperkuliahan(-1L);
				komentar.setDosen(dosen);
				komentar.setKomentar(KomentarHelper.this.komentar.getValue());
				komentar.setMahasiswa(mahasiswa);
				komentar.setSemester(semester);
				komentar.setTahapan(tahapan);
				komentar.setTahunAkademik(tahunAkademik);
				komentar.setTanggal(ais.ui.util.WaktuUtil.getDate());
				komentar.setTbmuser(tbmuser);

				komentar.setSemesterPendek(semesterPendek);

				KomentarDao komentarDao = DaoFactory.getInstance().getKomentarDao();
				komentarDao.save(komentar);

				if (!KomentarHelper.this.komentar.getValue().trim().isEmpty()) {
					Common.createDefaultTimer(new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {
							String emailUser = "";

							JSONArray userIds = new JSONArray();
							userIds.put(tbmuser.getUserId());

							if (dosenpa != null && dosenpa.getEmail() != null
									&& Common.isValidEmailAddress(dosenpa.getEmail())) {
								emailUser += emailUser.trim().isEmpty() ? dosenpa.getEmail().trim()
										: "," + dosenpa.getEmail().trim();
							}

							List<String> emails = HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("dosen", dosenpa))
									.setProjection(Projections.groupProperty("userId")).list();
							for (String email : emails) {
								userIds.put(email);
							}

							if (mahasiswa != null && mahasiswa.getEmail() != null
									&& Common.isValidEmailAddress(mahasiswa.getEmail())) {
								emailUser += emailUser.trim().isEmpty() ? mahasiswa.getEmail().trim()
										: "," + mahasiswa.getEmail().trim();
							}

							userIds.put(mahasiswa.getNim());

							if (tbmuser != null && tbmuser.getEmail() != null
									&& Common.isValidEmailAddress(tbmuser.getEmail())) {
								emailUser += emailUser.trim().isEmpty() ? tbmuser.getEmail().trim()
										: "," + tbmuser.getEmail().trim();
							}

							// System.out.println("emailUser = " + emailUser);

							if (!emailUser.trim().isEmpty() || userIds.length() > 0) {

								String info = "Mahasiswa: " + mahasiswa.getNim() + " " + mahasiswa.getNama()
										+ ", semester: " + semester + ", Tahun Akademik: " + tahunAkademik;
								String subject = "Komentar di KRS => " + info;

								File file = Report.generateFileReport(Report.PDF,
										CommonReportHelper.generateParameterKrs(mahasiswa, semester, tahapan,
												semesterPendek, remedial, false, false),
										"Cetak_KRS_Mahasiswa", ais.ui.util.WaktuUtil.getDate(), Common.locale);

								String body = "Anda mendapatkan informasi dari komentar KRS.<br>Komentar dari : "
										+ tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")"
										+ "<br>Isi komentar-nya adalah : " + KomentarHelper.this.komentar.getValue()
										+ "<br>Terlampir KRS yang Anda ambil.<br>Untuk informasi lebih lanjut bisa dilihat di "
										+ Common.getRequestHostWithProtocol()
										+ ", kemudian click menu KRS, cari komentar sbb : " + info
										+ "<br><br>Terima Kasih";

								String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
								MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser, komentar, file);
							}

							window.detach();
							eventListener.onEvent(new Event("", window, komentar));
						}
					});
				}
			}
		});
		save.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
	}

}
