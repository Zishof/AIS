package ais.action.master.sirs.chart.helper;

/**
 * Dulu subclass org.zkoss.zkex.zul.impl.JFreeChartEngine (PE-only) untuk
 * mewarnai dan memberi judul chart JFreeChart. Chart kini digambar oleh
 * ais.ui.util.MyChart (HTML/CSS, ZK CE) sehingga class ini cukup menjadi
 * pembawa judul; MyChart.setEngine() menerima objek ini sebagai no-op.
 */
public class KunjunganPasienRawatJalanChartHelper {

	private Integer tahun;

	public KunjunganPasienRawatJalanChartHelper(Integer tahun) {
		this.tahun = tahun;
	}

	public Integer getTahun() {
		return tahun;
	}

	public String getJudul() {
		return "Kunjungan Pasien Rawat Jalan Tahun " + tahun;
	}
}
