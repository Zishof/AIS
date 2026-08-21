package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/** Audit append-only untuk integrasi DOI, ORCID, ROR, COAR Notify, antivirus, dan AI. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=false)
@Table(schema="public",name="repo_integration_event")
public class RepoIntegrationEvent implements Serializable {
    private static final long serialVersionUID=1L;private Long id,itemId;private String tenantKey,serviceName,actionName,status,actorId,requestId,errorMessage;private String requestPayload,responsePayload;private Date createdAt;
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false)public Long getId(){return id;}public void setId(Long v){id=v;}
    @Column(name="item_id")public Long getItemId(){return itemId;}public void setItemId(Long v){itemId=v;}
    @Column(name="tenant_key",nullable=false,length=120)public String getTenantKey(){return tenantKey;}public void setTenantKey(String v){tenantKey=v;}
    @Column(name="service_name",nullable=false,length=60)public String getServiceName(){return serviceName;}public void setServiceName(String v){serviceName=v;}
    @Column(name="action_name",nullable=false,length=80)public String getActionName(){return actionName;}public void setActionName(String v){actionName=v;}
    @Column(name="status",nullable=false,length=30)public String getStatus(){return status;}public void setStatus(String v){status=v;}
    @Column(name="actor_id",length=255)public String getActorId(){return actorId;}public void setActorId(String v){actorId=v;}
    @Column(name="request_id",length=120)public String getRequestId(){return requestId;}public void setRequestId(String v){requestId=v;}
    @Column(name="request_payload",columnDefinition="TEXT")public String getRequestPayload(){return requestPayload;}public void setRequestPayload(String v){requestPayload=v;}
    @Column(name="response_payload",columnDefinition="TEXT")public String getResponsePayload(){return responsePayload;}public void setResponsePayload(String v){responsePayload=v;}
    @Column(name="error_message",columnDefinition="TEXT")public String getErrorMessage(){return errorMessage;}public void setErrorMessage(String v){errorMessage=v;}
    @Temporal(TemporalType.TIMESTAMP)@Column(name="created_at",nullable=false)public Date getCreatedAt(){return createdAt;}public void setCreatedAt(Date v){createdAt=v;}
}
