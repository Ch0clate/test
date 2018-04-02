package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;


@Entity
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
public abstract class ProductOrder implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@Column()
	@GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid",strategy = "uuid")
	protected String id;
	
	@OneToMany(mappedBy="order", cascade=CascadeType.ALL)
	private Set<SupplierCommercial> suppcommercial;
	
	@OneToMany(mappedBy="order", cascade=CascadeType.ALL)
	private Set<ClientCommercial> clientCommercial;
	
	@ManyToOne(fetch = FetchType.LAZY,cascade=CascadeType.ALL)
	@JoinColumn(name="bookID")
    public Booking booking;
	
	@Column
	private String status;

	
	@Column
	private String QCStatus;
	@Column
	private String clientReconfirmStatus;
	@Column
	private String productSubCategory;
	@Column
	private String suppReconfirmStatus;

	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Set<SupplierCommercial> getSuppcommercial() {
		return suppcommercial;
	}

	public void setSuppcommercial(Set<SupplierCommercial> suppcommercial) {
		this.suppcommercial = suppcommercial;
	}

	public Set<ClientCommercial> getClientCommercial() {
		return clientCommercial;
	}

	public void setClientCommercial(Set<ClientCommercial> clientCommercial) {
		this.clientCommercial = clientCommercial;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Booking getBooking() {
		return booking;
	}

	public void setBooking(Booking booking) {
		this.booking = booking;
	}

	public String getQCStatus() {
		return QCStatus;
	}

	public void setQCStatus(String qCStatus) {
		QCStatus = qCStatus;
	}

	public String getClientReconfirmStatus() {
		return clientReconfirmStatus;
	}

	public void setClientReconfirmStatus(String clientReconfirmStatus) {
		this.clientReconfirmStatus = clientReconfirmStatus;
	}

	public String getProductSubCategory() {
		return productSubCategory;
	}

	public void setProductSubCategory(String productSubCategory) {
		this.productSubCategory = productSubCategory;
	}

	public String getSuppReconfirmStatus() {
		return suppReconfirmStatus;
	}

	public void setSuppReconfirmStatus(String suppReconfirmStatus) {
		this.suppReconfirmStatus = suppReconfirmStatus;
	}
	

}
