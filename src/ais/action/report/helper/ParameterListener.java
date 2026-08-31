package ais.action.report.helper;

import java.io.Serializable;
import java.util.Map;

/**
 * Kontrak sederhana untuk penyedia parameter laporan: implementasi menghasilkan peta nama-parameter
 * ke nilainya yang akan disisipkan ke mesin pembuat laporan (mis. JasperReports) sebelum laporan
 * dirender. Dipakai oleh berbagai kelas helper laporan di paket {@code ais.action.report} yang
 * butuh menyusun parameter secara dinamis (query tambahan, filter, atau nilai turunan) di luar
 * parameter baku yang sudah dikumpulkan dari form.
 */
public interface ParameterListener {

	/**
	 * Menghasilkan peta parameter tambahan untuk laporan.
	 *
	 * @return peta nama parameter ke nilainya (harus {@link Serializable} agar aman dipakai mesin laporan)
	 * @throws Exception diteruskan apa adanya bila penyusunan parameter gagal (mis. query database gagal)
	 */
	public Map<String, Serializable> generateParameters() throws Exception;

}
