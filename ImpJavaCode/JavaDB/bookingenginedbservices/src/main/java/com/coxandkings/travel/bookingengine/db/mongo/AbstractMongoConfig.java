package com.coxandkings.travel.bookingengine.db.mongo;

import com.coxandkings.travel.bookingengine.db.utils.CryptoUtil;

public abstract class AbstractMongoConfig implements MongoConfig {
	protected String mMongoURL = "";
	protected String mMongoUser = "";
	protected String mMongoPwd = "";
	protected String mMongoDB = "";
	protected String mMongoURI = "";

	protected void formatMongoURI() {
		StringBuilder strBldr = new StringBuilder(mMongoURL);
		if (mMongoUser != null && mMongoUser.isEmpty() == false) {
			String mongoPwd = CryptoUtil.decrypt(mMongoPwd);
			int atCharIdx = strBldr.indexOf("@");
			if (atCharIdx > -1) {
				strBldr.replace("mongodb://".length(), atCharIdx, String.format("%s:%s", mMongoUser, mongoPwd));
			}
			else {
				strBldr.insert("mongodb://".length(), String.format("%s:%s", mMongoUser, mongoPwd));
			}
		}
		
		if (mMongoDB != null && mMongoDB.isEmpty() == false) {
			int slashIdx = strBldr.indexOf("/", "mongodb://".length());
			int questionIdx = strBldr.indexOf("?");
			if (slashIdx > -1) {
				if (questionIdx > -1) {
					strBldr.replace(slashIdx + 1, questionIdx, mMongoDB);
				}
				else {
					strBldr.replace(slashIdx + 1, strBldr.length(), mMongoDB);
				}
			}
			else {
				strBldr.append(String.format("/%s", mMongoDB));
			}
		}
		
		mMongoURI = strBldr.toString();
	}
	
	@Override
	public String getConnectionURI() {
		return mMongoURI;
	}

	public String getDatabaseName() {
		return mMongoDB;
	}
	
}
