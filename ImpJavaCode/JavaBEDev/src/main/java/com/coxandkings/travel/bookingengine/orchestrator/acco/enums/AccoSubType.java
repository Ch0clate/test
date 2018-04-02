package com.coxandkings.travel.bookingengine.orchestrator.acco.enums;

public enum AccoSubType {

	HOTEL("Hotel"),MOTEL("Motel"),RESORTS("Resorts"),HOSTELS("Hostels"),LODGE("Lodge"),GUESTHOUSE("GuestHouse"),HOUSEBOATS("HouseBoats"),
	APARTMENTS("Apartments"),BUNGALOWS("Bungalows"),COTTAGES("Cottages"),CHALET("Chalet"),VILLAS("Villas"),HOLIDAYHOME("HolidayHome"),
	MOBILEHOME("MobileHome"),TENTS("Tents"),CASTLESANDPALACES("CastlesAndPalaces");
	
	private String mSubCateg;
	private AccoSubType(String subCateg) {
		this.mSubCateg = subCateg;
	}
	
	public String toString() {
		return mSubCateg;
	}
	
	public static AccoSubType forString(String subCategClassStr) { 
		AccoSubType[] subCategories = AccoSubType.values();
		for (AccoSubType subCategory : subCategories) {
			if (subCategory.toString().equals(subCategClassStr)) {
				return subCategory;
			}
		}
		
		return null;
	}
}
