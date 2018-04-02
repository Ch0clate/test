package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name=  "AMCL")
public class AmCl extends AbstractAmCl implements Serializable {
	

	private static final long serialVersionUID = 1L;
	
}
