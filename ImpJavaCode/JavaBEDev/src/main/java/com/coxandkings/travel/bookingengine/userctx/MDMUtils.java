package com.coxandkings.travel.bookingengine.userctx;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.coxandkings.travel.bookingengine.config.MDMConfig;
import com.coxandkings.travel.bookingengine.utils.Constants;
import com.mongodb.DBCollection;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

public class MDMUtils implements Constants {
	
	private static Document getLatestUpdatedDocument(FindIterable<Document> resDocs) {
		long latestUpdatedTime = Instant.EPOCH.toEpochMilli();
		Document latestUpdatedDocument = null;
		MongoCursor<Document> resDocsIter = resDocs.iterator();
		while (resDocsIter.hasNext()) {
			Document resDoc = resDocsIter.next();
			Date docUpdatedDate = resDoc.getDate(MDM_PROP_LASTUPDATED);
			if (docUpdatedDate != null && docUpdatedDate.toInstant().toEpochMilli() > latestUpdatedTime) {
				latestUpdatedDocument = resDoc;
				latestUpdatedTime = docUpdatedDate.toInstant().toEpochMilli();
			}
		}
		
		return latestUpdatedDocument;
	}
	
	public static Document getSupplierCommercials(String prodCateg, String prodCategSubType, String suppID) {
		MongoCollection<Document> ctColl = MDMConfig.getCollection(MDM_COLL_SUPPCOMMS);

		Map<String, Object> filters = new HashMap<String, Object>();
		filters.put(MDM_PROP_DELETED, false);
		filters.put(MDM_PROP_COMMERCIALDEFN.concat(".").concat(MDM_PROP_PRODCATEG), prodCateg);
		filters.put(MDM_PROP_COMMERCIALDEFN.concat(".").concat(MDM_PROP_PRODCATEGSUBTYPE), prodCategSubType);
		filters.put(MDM_PROP_COMMERCIALDEFN.concat(".").concat(MDM_PROP_SUPPID), suppID);
		
		return getLatestUpdatedDocument(ctColl.find(new Document(filters)));
	}

	public static Document getSupplierCredentialsConfig(String suppID, String cred) {
		MongoCollection<Document> scColl = MDMConfig.getCollection(MDM_COLL_SUPPCREDS);
	
		Map<String, Object> filters = new HashMap<String, Object>();
		filters.put(MDM_PROP_DELETED, false);
		filters.put(DBCollection.ID_FIELD_NAME, cred);
		filters.put(MDM_PROP_SUPP.concat(".").concat(MDM_PROP_SUPPID), suppID);
		
		return getLatestUpdatedDocument(scColl.find(new Document(filters)));
	}

	// As of now ProductCategory and ProductCategorySibType are not used because MDM collection does not have 
	// these attributes. However, as per CKIL, supplier settlement terms should be defined at product category/
	// product category subtype and supplierId level.  
	static Document getSupplierSettlementTerms(String prodCateg, String prodCategSubType, String suppID) {
		MongoCollection<Document> scColl = MDMConfig.getCollection(MDM_COLL_SUPPSETTLES);
	
		Map<String, Object> filters = new HashMap<String, Object>();
		filters.put(MDM_PROP_DELETED, false);
		filters.put(MDM_PROP_SUPPID, suppID);
		
		return getLatestUpdatedDocument(scColl.find(new Document(filters)));
	}

	public static String getValueAtPathFromDocument(Document doc, String path) {
		if (doc == null || path == null || path.isEmpty()) {
			return "";
		}
		
		int dotIdx = path.indexOf('.');
		return (dotIdx == -1) ? ((doc.containsKey(path)) ? doc.getString(path) : "") : getValueAtPathFromDocument((Document) doc.get(path.substring(0, dotIdx)), path.substring(dotIdx + 1));
	}

	public static Object getValueObjectAtPathFromDocument(Document doc, String path) {
		if (doc == null || path == null || path.isEmpty()) {
			return null;
		}
		
		int dotIdx = path.indexOf('.');
		return (dotIdx == -1) ? ((doc.containsKey(path)) ? doc.get(path) : null) : getValueObjectAtPathFromDocument((Document) doc.get(path.substring(0, dotIdx)), path.substring(dotIdx + 1));
	}
	
	@SuppressWarnings("unchecked")
	public static Document getStandardCommercialSettlementTerms(Document suppSettlementDoc, boolean isCommissionable) {
		if (suppSettlementDoc == null) {
			return null;
		}
		
		Document configDoc = (Document) ((isCommissionable) ? suppSettlementDoc.get(MDM_PROP_COMMISSIONCOMMS) : suppSettlementDoc.get(MDM_PROP_NONCOMMISSIONCOMMS));
		if (configDoc == null) {
			return null;
		}
		
		List<Document> commHeadDocs = (List<Document>) configDoc.get(MDM_PROP_COMMERCIALHEADS);
		if (commHeadDocs == null) {
			return null;		
		}
		
		for (Document commHeadDoc : commHeadDocs) {
			String commHead = (String) commHeadDoc.getOrDefault(MDM_PROP_COMMERCIALHEAD, "");
			if ((isCommissionable) ? MDM_VAL_STDCOMMCOMMISSION.equals(commHead) : MDM_VAL_STDCOMMNONCOMMISSION.equals(commHead)) {
				return commHeadDoc;
			}
		}
			
		return null;
	}

	public static Document getSupplier(String suppID) {
		MongoCollection<Document> suppColl = MDMConfig.getCollection(MDM_COLL_SUPPS);

		Map<String, Object> filters = new HashMap<String, Object>();
		filters.put(MDM_PROP_DELETED, false);
		filters.put(DBCollection.ID_FIELD_NAME, suppID);
		
		return getLatestUpdatedDocument(suppColl.find(new Document(filters)));
	}
	
	public static Document getCorpTraveller(String clientType, String userID) {
		MongoCollection<Document> corpTrvlrsColl = MDMConfig.getCollection(MDM_COLL_CORPTRAVELLERS);

		Map<String, Object> filters = new HashMap<String, Object>();
		filters.put(MDM_PROP_DELETED, false);
		filters.put(MDM_PROP_CLIENTDETAILS.concat(".").concat(MDM_PROP_CLIENTTYPE), clientType);
		filters.put(MDM_PROP_TRAVELLERDETAILS.concat(".").concat(MDM_PROP_USERID), userID);
		
		return getLatestUpdatedDocument(corpTrvlrsColl.find(new Document(filters)));
	}
	
	public static Document getClientLocation(String clientLocID) {
		MongoCollection<Document> clientLocsColl = MDMConfig.getCollection(MDM_COLL_CORPTRAVELLERS);

		Map<String, Object> filters = new HashMap<String, Object>();
		filters.put(MDM_PROP_DELETED, false);
		filters.put(DBCollection.ID_FIELD_NAME, clientLocID);
		
		return getLatestUpdatedDocument(clientLocsColl.find(new Document(filters)));
	}
	
}
