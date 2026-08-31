/**
 * BillPaymentService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ws.billpayment.h2h.bankmandiri;

/**
 * Kontrak factory/locator JAX-RPC untuk layanan bill-payment. Pemanggil memakainya untuk
 * memperoleh port SOAP tanpa bergantung langsung pada kelas stub Apache Axis.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini mendeklarasikan kontrak {@link javax.xml.rpc.Service}. Implementasi
 * konkret bertanggung jawab atas transaksi, resource, error handling, dan efek samping; pemanggil sebaiknya
 * bergantung pada kontrak ini agar tidak menggandakan integrasi.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getBillPaymentServiceSoapAddress()},
 * {@code getBillPaymentServiceSoap()}, {@code getBillPaymentServiceSoap()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> bergantung pada perannya, operasi dapat mengubah konfigurasi endpoint, membuat stub,
 * melakukan I/O jaringan, atau meneruskan request ke logika transaksi. Error transport tetap diteruskan sebagai
 * kontrak JAX-RPC/Axis; jangan menggandakan mapping WSDL di kelas lain.</p>
 */
public interface BillPaymentService extends javax.xml.rpc.Service {
    public java.lang.String getBillPaymentServiceSoapAddress();

    public ws.billpayment.h2h.bankmandiri.BillPaymentServiceSoap getBillPaymentServiceSoap() throws javax.xml.rpc.ServiceException;

    public ws.billpayment.h2h.bankmandiri.BillPaymentServiceSoap getBillPaymentServiceSoap(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
