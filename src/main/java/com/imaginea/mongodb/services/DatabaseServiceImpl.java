/*
 * Copyright (c) 2011 Imaginea Technologies Private Ltd.
 * Hyderabad, India
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.imaginea.mongodb.services;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
 
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.imaginea.mongodb.common.MongoInstanceProvider;
import com.imaginea.mongodb.common.SessionMongoInstanceProvider;
import com.imaginea.mongodb.common.exceptions.DatabaseException;
import com.imaginea.mongodb.common.exceptions.DeleteDatabaseException;
import com.imaginea.mongodb.common.exceptions.DuplicateDatabaseException;
import com.imaginea.mongodb.common.exceptions.EmptyDatabaseNameException;
import com.imaginea.mongodb.common.exceptions.ErrorCodes;
import com.imaginea.mongodb.common.exceptions.InsertDatabaseException;
import com.imaginea.mongodb.common.exceptions.UndefinedDatabaseException;
import com.imaginea.mongodb.common.exceptions.ValidationException;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

/**
 * Defines services definitions for performing operations like create/drop on
 * databases present in mongo to which we are connected to. Also provides service
 * to get list of all databases present and Statistics of a particular database.
 * 
 * @author Rachit Mittal
 * @since  2 July 2011
 * 
 * 
 */
public class DatabaseServiceImpl implements DatabaseService {

	/**
	 * Instance variable used to get a mongo instance after binding to an
	 * implementation.
	 */
	private MongoInstanceProvider mongoInstanceProvider;
	/**
	 * Mongo Instance to communicate with mongo
	 */
	private Mongo mongoInstance;

	/**
	 * Creates an instance of MongoInstanceProvider which is used to get a mongo
	 * instance to perform operations on databases. The instance is created
	 * based on a userMappingKey which is recieved from the database request
	 * dispatcher and is obtained from tokenId of user.
	 * 
	 * @param userMappingKey
	 *            A combination of username,mongoHost and mongoPort
	 */
	public DatabaseServiceImpl(String userMappingKey) {
		mongoInstanceProvider = new SessionMongoInstanceProvider(userMappingKey);
	 
	}

	/**
	 * Gets the list of databases present in mongo database to which user is
	 * connected to.
	 * 
	 * @return List of All Databases present in MongoDb
	 * 
	 * @throws DatabaseException
	 *             If any error while getting database list.
	 */
 
	public List<String> getDbList() throws DatabaseException {

		mongoInstance = mongoInstanceProvider.getMongoInstance();
		List<String> dbNames;
		try {
			dbNames = mongoInstance.getDatabaseNames();
		} catch (MongoException m) {
			DatabaseException e = new DatabaseException(ErrorCodes.GET_DB_LIST_EXCEPTION, "GET_DB_LIST_EXCEPTION", m.getCause());
			throw e;
		}
		return dbNames;

	}

	/**
	 * Creates a Database with the specified name in mongo database to which
	 * user is connected to.
	 * 
	 * @param dbName
	 *            Name of Database to be created
	 * @return Success if Created else throws Exception
	 * 
	 * @exception EmptyDatabaseNameException
	 *                When dbName is null
	 * @exception DuplicateDatabaseException
	 *                When database is already present
	 * @exception InsertDatabaseException
	 *                Any exception while trying to create db
	 * @exception DatabaseException
	 *                throw super type of
	 *                DuplicateDatabaseException,InsertDatabaseException
	 * @exception ValidationException
	 *                throw super type of EmptyDatabaseNameException
	 * 
	 * 
	 */

	public String createDb(String dbName) throws DatabaseException, ValidationException {
		mongoInstance = mongoInstanceProvider.getMongoInstance();

		if (dbName == null) {
			throw new EmptyDatabaseNameException("Database name is null");

		}
		if (dbName.equals("")) {
			throw new EmptyDatabaseNameException("Database Name Empty");
		}

		try {
			boolean dbAlreadyPresent = mongoInstance.getDatabaseNames().contains(dbName);
			if (dbAlreadyPresent) {
				throw new DuplicateDatabaseException("DB with name [" + dbName + "] ALREADY EXISTS");
			}

			mongoInstance.getDB(dbName).getCollectionNames();
		} catch (MongoException e) {

			throw new InsertDatabaseException("DB_CREATION_EXCEPTION", e.getCause());
		}

		String result = "Created DB with name [" + dbName + "]";
		return result;
	}

	/**
	 * Deletes a Database with the specified name in mongo database to which
	 * user is connected to.
	 * 
	 * @param dbName
	 *            Name of Database to be deleted
	 * @return Success if deleted else throws Exception
	 * 
	 * @exception EmptyDatabaseNameException
	 *                When dbName is null
	 * @exception UndefinedDatabaseException
	 *                When database is not present
	 * @exception DeleteDatabaseException
	 *                Any exception while trying to create db
	 * @exception DatabaseException
	 *                throw super type of
	 *                UndefinedDatabaseException,DeleteDatabaseException
	 * @exception ValidationException
	 *                throw super type of EmptyDatabaseNameException
	 * 
	 * 
	 */
	public String dropDb(String dbName) throws DatabaseException, ValidationException {
		mongoInstance = mongoInstanceProvider.getMongoInstance();
		if (dbName == null) {
			throw new EmptyDatabaseNameException("Database name is null");

		}
		if (dbName.equals("")) {
			throw new EmptyDatabaseNameException("Database Name Empty");
		}
		try {
			boolean dbPresent = mongoInstance.getDatabaseNames().contains(dbName);
			if (!dbPresent) {
				throw new UndefinedDatabaseException("DB with name [" + dbName + "]  DOES NOT EXIST");
			}

			mongoInstance.dropDatabase(dbName);
		} catch (MongoException e) {

			throw new DeleteDatabaseException("DB_DELETION_EXCEPTION", e.getCause());
		}

		String result = "Deleted DB with name [" + dbName + "]";
		return result;
	}

	/**
	 * Return Stats of a particular Database in mongo to which user is connected
	 * to.
	 * 
	 * @param dbName
	 *            Name of Database
	 * @return Array of JSON Objects each containing a key value pair in Db
	 *         Stats.
	 * @exception EmptyDatabaseNameException
	 *                DbName is empty
	 * @exception UndefinedDatabaseException
	 *                Db not present
	 * @exception JSONException
	 *                While parsing JSON
	 * @exception DatabaseException
	 *                Error while performing this operation
	 * @exception ValidationException
	 *                throw super type of EmptyDatabaseNameException
	 */
	 
	public JSONArray getDbStats(String dbName) throws DatabaseException, ValidationException, JSONException {

		mongoInstance = mongoInstanceProvider.getMongoInstance();
		if (dbName == null) {
			throw new EmptyDatabaseNameException("Database name is null");

		}
		if (dbName.equals("")) {
			throw new EmptyDatabaseNameException("Database Name Empty");
		}

		JSONArray dbStats = new JSONArray();
		try {
			boolean dbPresent = mongoInstance.getDatabaseNames().contains(dbName);
			if (!dbPresent) {
				throw new UndefinedDatabaseException("DB with name [" + dbName + "]  DOES NOT EXIST");
			}

			DB db = mongoInstance.getDB(dbName);
			CommandResult stats = db.getStats();
			Set<String> keys = stats.keySet();

			Iterator<String> keyIterator = keys.iterator();

			JSONObject temp = new JSONObject();

			while (keyIterator.hasNext()) {
				temp = new JSONObject();
				String key = keyIterator.next().toString();
				temp.put("Key", key);
				String value = stats.get(key).toString();
				temp.put("Value", value);
				String type = stats.get(key).getClass().toString();
				temp.put("Type", type.substring(type.lastIndexOf('.') + 1));
				dbStats.put(temp);
			}
		} catch (JSONException e) {
			throw e;
		} catch (MongoException m) {
			throw new DatabaseException(ErrorCodes.GET_DB_STATS_EXCEPTION, "GET_DB_STATS_EXCEPTION");
		}

		return dbStats;
	}
}
