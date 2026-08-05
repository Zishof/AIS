package ais.ui.util;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import org.zkoss.zul.Vbox;

import ais.action.master.helper.KrsHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;

public class KrsMahasiswaDataRenderer extends ais.ui.util.MyRowRenderer {

	private boolean keDatabase;
	private Mahasiswa mahasiswa;
	private Integer semesterPendek;
	private boolean rinci;
	private boolean remedial;

	public KrsMahasiswaDataRenderer(boolean keDatabase, Mahasiswa mahasiswa, Integer semesterPendek, boolean remedial,
			boolean rinci) {
		this.keDatabase = keDatabase;
		this.mahasiswa = mahasiswa;
		this.semesterPendek = semesterPendek;
		this.rinci = rinci;
		this.remedial = remedial;
	}

	@Override
	public void render(final Row arg0, Object arg1) throws Exception {
		arg0.setValign("top");
		// TODO Auto-generated method stub
		final String[] data = (String[]) arg1;
		final Boolean editable = true;
		final MyDetail detail = new MyDetail();
		Integer smt;
		try {
			smt = Integer.parseInt(data[1].split(",")[0]);
		} catch (Exception e) {
			smt = 0;
		}
		final Integer semester = smt;

		Integer tahap;
		try {
			tahap = Integer.parseInt(data[3]);
		} catch (Exception e) {
			tahap = 0;
		}
		final Integer tahapan = tahap;

		final String tahunAjaran = data[0];

		final Html html = new ais.ui.util.MyHtml("");
		final Html komentarshtml = new ais.ui.util.MyHtml("");
		detail.setParent(arg0);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(detail);
				if (detail.isOpen()) {

					KrsHelper krsHelper = new KrsHelper(semesterPendek, remedial);
					krsHelper.display(editable, mahasiswa, tahunAjaran, semester, tahapan, detail, html, komentarshtml,
							keDatabase);
				}
			}
		};
		detail.addEventListener("onOpen", eventListener);

		new Label(data[0]).setParent(arg0);
		new Label(tahapan != null && tahapan.equals(-1) ? "" : semester.equals(1000) ? "Lulus" : data[1])
				.setParent(arg0);
		new Label(tahapan != null && tahapan.equals(-1) ? "" : tahapan + "").setParent(arg0);
		try {
			new Label(data[2]).setParent(arg0);
		} catch (Exception e) {
			new Label().setParent(arg0);
		}

		final Label ip = new Label();
		ip.setParent(arg0);
		final MyLabelAgakKecil sks = new MyLabelAgakKecil();
		sks.setParent(arg0);

		html.setParent(arg0);
		komentarshtml.setParent(arg0);

		Vbox vbox = new Vbox();
		vbox.setParent(arg0);
		final MyLabelAgakKecil catatan = new MyLabelAgakKecil();
		final MyLabelAgakKecil catatanKhs = new MyLabelAgakKecil();
		catatan.setParent(vbox);
		catatanKhs.setParent(vbox);

		if (rinci) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event a) throws Exception {

					if (!ConstantValues.aktifkanTahapan) {
						String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
						if (Common
								.getSemester(mahasiswa.getTahunangkatan(), semesterMulai,
										mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai())
								.equals(semester)) {
							arg0.setStyle("border:0px;background: #C2FFA3;");
							detail.setOpen(true);
							eventListener.onEvent(null);
						}
					} else {
						Integer t = mahasiswa.currentTahapan();
						if (t != null && !t.equals(0) && tahapan != null && !tahapan.equals(0) && tahapan.equals(t)) {
							arg0.setStyle("border:0px;background: #C2FFA3;");
							detail.setOpen(true);
							eventListener.onEvent(null);
						}
					}

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan,
							semesterPendek, keDatabase);
					catatan.setValue(krsMahasiswa.getCatatan());
					catatanKhs.setValue(krsMahasiswa.getCatatanKhs());
					String krs = mahasiswa.rubahKeteranganPengambilanKRS(semester, tahapan, semesterPendek,
							krsMahasiswa, remedial);
					html.setContent(krs);
					Integer komentars = krsMahasiswa.getKomentars();

					String kom = komentars == 0 ? "Tidak ada komentar" : "Terdapat " + komentars + " komentar";
					komentarshtml.setContent(kom);

					if (semester > 0) {
						Double ipmhs = krsMahasiswa.getIps();
						Double ipkmhs = krsMahasiswa.getIpk();
						ip.setValue(Common.numberFormat.get().format(ipmhs) + " / " + Common.numberFormat.get().format(ipkmhs));

						Integer sksmhss = krsMahasiswa.getSksYangDiambil();
						Integer sksmhs = krsMahasiswa.getSksk();
						Integer skskonversi = krsMahasiswa.getSksKonversi();
						Integer sksBukanKonversi = krsMahasiswa.getSksBukanKonversi();
						sks.setValue(Common.numberFormat.get().format(sksmhss) + " / " + Common.numberFormat.get().format(sksmhs)
								+ (skskonversi > 0
										? " (Bukan Konversi : " + Common.numberFormat.get().format(sksBukanKonversi)
												+ " SKS, Konversi " + Common.numberFormat.get().format(skskonversi) + " SKS)"
										: ""));
					}
				}
			});
		}
	}
}
