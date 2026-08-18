package ais.action.master.bkd.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jenjang;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;

public class PenilaianAsesorHelper {

	public static MyDiv formNilai(Pegawai pegawai, final Jenjang jenjang, final String tahunAkademik,
			final String semester, final String buktiDokumen, final String spesifikasi,
			final EventListener keteranganEventListener) throws Exception {
		return formNilai(pegawai, null, null, jenjang, tahunAkademik, semester, buktiDokumen, spesifikasi,
				keteranganEventListener);
	}

	@SuppressWarnings("unchecked")
	public static MyDiv formNilai(Pegawai pegawai, final String namaKolom, final GeneralValueObject data,
			final Jenjang jenjang, final String tahunAkademik, final String semester, final String buktiDokumen,
			final String spesifikasi, final EventListener keteranganEventListener) throws Exception {
		Session session = HibernateUtil.currentSession();
		Tbmuser tbmuser = Common.getCurrentUser();

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 300px;");
		groupbox.setWidth("95%");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pilih");
		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Masa Tugas");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS Beban");
		column.setWidth("8%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS Kinerja");
		column.setWidth("8%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Bukti");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Catatan");
		column.setWidth("40%");

		Rows rows = new Rows();
		rows.setParent(grid);

		List<AsesemenPenilaian> asesemenPenilaians = session.createCriteria(AsesemenPenilaian.class)
				.add(Restrictions.eq("spesifikasi", spesifikasi))
				.add(jenjang == null ? Restrictions.isNull("jenjang") : Restrictions.eq("jenjang", jenjang))
				.add(namaKolom == null ? Restrictions.sqlRestriction("true") : Restrictions.eq(namaKolom, data))

				.createAlias("pegawai", "pegawai")

				.add(Restrictions.or(Restrictions.eq("pegawai.dosen", pegawai.getDosen()),
						Restrictions.eq("pegawai", pegawai)))

				.add(Restrictions.eq("tahunAkademik", tahunAkademik)).add(Restrictions.eq("semester", semester)).list();

		List<Asesor> merupakanAsesor = session.createCriteria(AsesorPegawai.class)

				.createAlias("pegawai", "pegawai")

				.add(Restrictions.or(Restrictions.eq("pegawai.dosen", pegawai.getDosen()),
						Restrictions.eq("pegawai", pegawai)))

				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)))
				.add(Restrictions.eq("asesor.tbmuser", tbmuser)).setProjection(Projections.groupProperty("asesor"))
				.list();
		if (!merupakanAsesor.isEmpty()) {
			for (final AsesemenPenilaian asesemenPenilaian : asesemenPenilaians) {

				List<PenilaianAsesor> penilaianAsesors = session.createCriteria(PenilaianAsesor.class)
						.add(Restrictions.in("asesor", merupakanAsesor))
						.add(Restrictions.eq("asesemenPenilaian", asesemenPenilaian)).list();
				for (final PenilaianAsesor penilaianAsesor : penilaianAsesors) {

					Row row = new Row();row.setValign("top");
					row.setParent(rows);
					final MyCheckboxConfig pilih = new MyCheckboxConfig();
					pilih.setChecked(penilaianAsesor.getPilih());
					row.appendChild(pilih);

					final Textbox masaTugas = new Textbox(penilaianAsesor.getAsesemenPenilaian().getMasaTugas());
					final MyDoublebox sks = new MyDoublebox(penilaianAsesor.getSks());
					final Textbox bukti = new Textbox(penilaianAsesor.getBukti());
					final Textbox keterangan = new Textbox(penilaianAsesor.getKeterangan());

					final EventListener eventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							penilaianAsesor.setPilih(pilih.isChecked());
							penilaianAsesor.setKeterangan(keterangan.getValue().trim());
							penilaianAsesor.setBukti(bukti.getValue().trim());
							penilaianAsesor.setSks(sks.getValue());

							Common.refreshSaveOrUpdate(penilaianAsesor);
							Common.createDefaultTimerNoBusy(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									keteranganEventListener.onEvent(arg0);
								}
							}, "", false, 500);
						}
					};

					row.appendChild(new Label(penilaianAsesor.getAsesor().getAsesorPenunjangKinerjaDosen().getKode()));
					row.appendChild(new Label(penilaianAsesor.getAsesor().getAsesorPenunjangKinerjaDosen().getNama()));

					masaTugas.setDisabled(!penilaianAsesor.getPilih());
					masaTugas.setWidth("90%");
					masaTugas.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							AsesemenPenilaian asesemenPenilaian = penilaianAsesor.getAsesemenPenilaian();
							asesemenPenilaian.setMasaTugas(masaTugas.getValue());
							Common.refreshSaveOrUpdate(asesemenPenilaian);
						}
					});
					row.appendChild(masaTugas);

					row.appendChild(new Label(Common.numberFormat.get().format(asesemenPenilaian.getSks())));

					sks.setDisabled(!penilaianAsesor.getPilih());
					sks.setStyle("text-align: right;");
					sks.setWidth("90%");
					sks.addEventListener("onChange", eventListener);
					row.appendChild(sks);

					bukti.setDisabled(!penilaianAsesor.getPilih());
					bukti.setWidth("90%");
					bukti.setRows(2);
					bukti.addEventListener("onChange", eventListener);
					row.appendChild(bukti);

					keterangan.setDisabled(!penilaianAsesor.getPilih());
					keterangan.setWidth("90%");
					keterangan.setRows(2);
					keterangan.addEventListener("onChange", eventListener);
					row.appendChild(keterangan);

					pilih.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							sks.setDisabled(!pilih.isChecked());
							bukti.setValue(pilih.isChecked() ? buktiDokumen : "");

							bukti.setDisabled(!pilih.isChecked());
							keterangan.setDisabled(!pilih.isChecked());
							masaTugas.setDisabled(!pilih.isChecked());
							penilaianAsesor.setPilih(pilih.isChecked());

							eventListener.onEvent(arg0);
						}
					});

				}
			}

		} else {
			for (final AsesemenPenilaian asesemenPenilaian : asesemenPenilaians) {
				List<PenilaianAsesor> penilaianAsesors = session.createCriteria(PenilaianAsesor.class)
						.add(Restrictions.eq("asesemenPenilaian", asesemenPenilaian)).list();
				for (final PenilaianAsesor penilaianAsesor : penilaianAsesors) {

					Row row = new Row();row.setValign("top");
					row.setParent(rows);
					final MyCheckboxConfig pilih = new MyCheckboxConfig();
					pilih.setChecked(penilaianAsesor.getPilih());
					pilih.setDisabled(true);
					row.appendChild(pilih);

					row.appendChild(new Label(penilaianAsesor.getAsesor().getAsesorPenunjangKinerjaDosen().getKode()));
					row.appendChild(new Label(penilaianAsesor.getAsesor().getAsesorPenunjangKinerjaDosen().getNama()));
					row.appendChild(new Label(penilaianAsesor.getAsesemenPenilaian().getMasaTugas()));
					row.appendChild(new Label(Common.numberFormat.get().format(asesemenPenilaian.getSks())));
					row.appendChild(new Label(Common.numberFormat.get().format(penilaianAsesor.getSks())));
					row.appendChild(new Label(penilaianAsesor.getBukti()));
					row.appendChild(new Label(penilaianAsesor.getKeterangan()));
				}
			}
		}

		return groupbox;
	}

}
