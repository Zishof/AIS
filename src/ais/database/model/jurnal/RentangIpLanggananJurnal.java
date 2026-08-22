package ais.database.model.jurnal;
import javax.persistence.*;
@Entity @Table(schema="penelitiandanpengabdian",name="rentang_ip_langganan_jurnal")
public class RentangIpLanggananJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long langgananId; private Integer addressFamily; private String startAddress,endAddress,label;
 @Column(name="langganan_id",nullable=false) public Long getLanggananId(){return langgananId;} public void setLanggananId(Long v){langgananId=v;}
 @Column(name="address_family",nullable=false) public Integer getAddressFamily(){return addressFamily;} public void setAddressFamily(Integer v){addressFamily=v;}
 @Column(name="start_address",nullable=false,length=45) public String getStartAddress(){return startAddress;} public void setStartAddress(String v){startAddress=v;}
 @Column(name="end_address",nullable=false,length=45) public String getEndAddress(){return endAddress;} public void setEndAddress(String v){endAddress=v;}
 @Column(name="label",length=255) public String getLabel(){return label;} public void setLabel(String v){label=v;}
}
