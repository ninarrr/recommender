/* Copyright (C) 2014 
"Kompetenzzentrum fuer wissensbasierte Anwendungen Forschungs- und EntwicklungsgmbH" 
(Know-Center), Graz, Austria, office@know-center.at.

Licensees holding valid Know-Center Commercial licenses may use this file in
accordance with the Know-Center Commercial License Agreement provided with 
the Software or, alternatively, in accordance with the terms contained in
a written agreement between Licensees and Know-Center.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.eexcess.federatedrecommender;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import eu.eexcess.config.FederatedRecommenderConfiguration;
import eu.eexcess.dataformats.PartnerBadge;
import eu.eexcess.dataformats.PartnerBadgeStats;
import eu.eexcess.dataformats.RecommenderStats;
import eu.eexcess.dataformats.result.ResultList;
import eu.eexcess.dataformats.result.ResultStats;
import eu.eexcess.dataformats.userprofile.SecureUserProfile;
import eu.eexcess.dataformats.userprofile.SecureUserProfileEvaluation;
import eu.eexcess.federatedrecommender.dataformats.PartnersFederatedRecommendations;
import eu.eexcess.federatedrecommender.dbpedia.DbPediaSolrIndex;
import eu.eexcess.federatedrecommender.decomposer.DBPediaDecomposer;
import eu.eexcess.federatedrecommender.decomposer.PseudoRelevanceSourcesDecomposer;
import eu.eexcess.federatedrecommender.decomposer.PseudoRelevanceWikipediaDecomposer;
import eu.eexcess.federatedrecommender.decomposer.SerendiptiyDecomposer;
import eu.eexcess.federatedrecommender.interfaces.PartnersFederatedRecommendationsPicker;
import eu.eexcess.federatedrecommender.interfaces.SecureUserProfileDecomposer;
import eu.eexcess.federatedrecommender.picker.FiFoPicker;
import eu.eexcess.federatedrecommender.picker.OccurrenceProbabilityPicker;
import eu.eexcess.federatedrecommender.picker.SimilarityDiversificationPicker;
import eu.eexcess.federatedrecommender.registration.PartnerRegister;
import eu.eexcess.federatedrecommender.utils.FederatedRecommenderException;
import eu.eexcess.sqlite.Database;
import eu.eexcess.sqlite.DatabaseQueryStats;

/**
 * FederatedRecommenderCore (Singleton)
 * 
 */
public class FederatedRecommenderCore {

	private static final Logger logger = Logger
			.getLogger(FederatedRecommenderCore.class.getName());
	private static final String[] SUPPORTED_LOCALES = new String[] { "en", "de" };

	private static volatile FederatedRecommenderCore instance;
	private final FederatedRecommenderConfiguration federatedRecConfiguration;

	private PartnerRegister partnerRegister = new PartnerRegister();
	private ExecutorService threadPool;
	private final DbPediaSolrIndex dbPediaSolrIndex;
	private RecommenderStats recommenderStats;

	private FederatedRecommenderCore(
			FederatedRecommenderConfiguration federatedRecConfiguration) {
		threadPool = Executors
				.newFixedThreadPool(federatedRecConfiguration.numRecommenderThreads);
		this.federatedRecConfiguration = federatedRecConfiguration;
		this.dbPediaSolrIndex = new DbPediaSolrIndex(federatedRecConfiguration);
		this.recommenderStats = new RecommenderStats();
	}

	/**
	 * returns the registered partners in the system
	 * 
	 * @return
	 */
	public PartnerRegister getPartnerRegister() {
		synchronized (partnerRegister) {
			return partnerRegister;
		}
	}

	/**
	 * adds a partner to the system
	 * 
	 * @param badge
	 */
	public void addPartner(PartnerBadge badge) {
		synchronized (partnerRegister) {
			partnerRegister.addPartner(badge);
		}
	}

	/**
	 * returns the instance of the {@link FederatedRecommenderCore} has to be
	 * lazy caused by the configuration by now
	 * 
	 * @return
	 * @throws FederatedRecommenderException
	 */
	public static FederatedRecommenderCore getInstance(
			FederatedRecommenderConfiguration federatedRecommenderConfiguration)
			throws FederatedRecommenderException {
		if (instance == null) {
			synchronized (FederatedRecommenderCore.class) {
				// Double check
				if (instance == null) {
					instance = new FederatedRecommenderCore(
							federatedRecommenderConfiguration);
				}
			}
		}
		return instance;
	}

	/**
	 * returns the rcommendations for all registered partners as
	 * {@link PartnersFederatedRecommendations} object (containing a Hashmap
	 * with the partner as key and the data as value)
	 * 
	 * @param secureUserProfile
	 * @return
	 */
	public PartnersFederatedRecommendations getPartnersRecommendations(
			final SecureUserProfile secureUserProfile) {
		final PartnersFederatedRecommendations partnersFederatedResults = new PartnersFederatedRecommendations();

		long start = System.currentTimeMillis();
		Map<PartnerBadge, Future<ResultList>> futures = new HashMap<PartnerBadge, Future<ResultList>>();
		for (final PartnerBadge partner : partnerRegister.getPartners()) {
			if (checkUserSelectedPartners(secureUserProfile, partner)) {
				final Client tmpClient = partnerRegister.getClient(partner);

				Future<ResultList> future = threadPool
						.submit(new Callable<ResultList>() {
							@Override
							public ResultList call() throws Exception {
								long startTime = System.currentTimeMillis();
								ResultList resultList = getPartnerResult(
										partner, tmpClient, secureUserProfile);
								long endTime = System.currentTimeMillis();
								long respTime = endTime - startTime;
								partner.updatePartnerResponseTime(respTime);
								return resultList;
							}

							/**
							 * trys to recieve the results from the partners
							 * 
							 * @param partner
							 * @param secureUserProfile
							 * @return
							 */
							private ResultList getPartnerResult(
									PartnerBadge partner, Client client,
									SecureUserProfile secureUserProfile) {
								ResultList resultList = new ResultList();
								if (client != null) {
									try {
										WebResource resource = client.resource(partner
												.getPartnerConnectorEndpoint());
										resource.accept(MediaType.APPLICATION_JSON);
										resultList = resource.post(
												ResultList.class,
												secureUserProfile);
									} catch (Exception e) {
										logger.log(Level.WARNING, "Partner: "
												+ partner.getSystemId()
												+ " is not working currently.",
												e);
										throw e;
									}
								}
								client.destroy();
								return resultList;
							}
						});
				futures.put(partner, future);

			}
		}

		long timeout = federatedRecConfiguration.partnersTimeout; // ms
		for (Entry<PartnerBadge, Future<ResultList>> entry : futures.entrySet()) {
			long startT = System.currentTimeMillis();
			try {
				entry.getKey().shortTimeStats.requestCount++;
				ResultList rL = entry.getValue().get(timeout,
						TimeUnit.MILLISECONDS);
				entry.getValue().cancel(true);
				partnersFederatedResults.getResults().put(entry.getKey(), rL);
				entry.getKey().addLastQueries(rL.getResultStats());

				timeout -= System.currentTimeMillis() - startT;

				timeout = timeout - (System.currentTimeMillis() - startT);

			} catch (TimeoutException e) {
				entry.getKey().shortTimeStats.failedRequestCount++;
				entry.getKey().shortTimeStats.failedRequestTimeoutCount++;
				entry.getValue().cancel(true);
				timeout -= System.currentTimeMillis() - startT;
				logger.log(Level.WARNING,
						"Waited too long for partner system '" + entry.getKey()
								+ "' to respond " + timeout, e);
			} catch (Exception e) {
				if (entry.getKey() != null) {
					entry.getKey().shortTimeStats.failedRequestCount++;
					entry.getValue().cancel(true);
					timeout -= System.currentTimeMillis() - startT;
				}
				logger.log(Level.SEVERE,
						"Failed to retrieve results from a parter system '"
								+ entry.getKey() + "'" + (timeout) + "ms ", e);
			}
			entry.setValue(null);

		}
		long end = System.currentTimeMillis();
		logger.log(Level.INFO, "Federated Recommender took " + (end - start)
				+ "ms for query '" + secureUserProfile.contextKeywords + "'");

		return partnersFederatedResults;
	}

	/**
	 * checks which partners are selected and if there is an partner access key
	 * for that partner if one is needed
	 * 
	 * @param secureUserProfile
	 * @param partner
	 * @return true/false
	 */
	private boolean checkUserSelectedPartners(
			SecureUserProfile secureUserProfile, PartnerBadge partner) {
		if (secureUserProfile.partnerList != null) { // if the list is null then
														// we query every
														// partner
			if (!secureUserProfile.partnerList.isEmpty()) {
				boolean withKey = false;
				if (partner.partnerKey != null)
					if (!partner.partnerKey.isEmpty())
						withKey = true;
				if (!withKey)
					for (PartnerBadge uBadge : secureUserProfile.partnerList) {
						if (uBadge.getSystemId().equals(partner.getSystemId()))
							return true;
					}
				else
					for (PartnerBadge uBadge : secureUserProfile.protectedPartnerList) {
						if (uBadge.partnerKey != null)
							if (!uBadge.partnerKey.isEmpty())
								if (partner.partnerKey
										.equals(uBadge.partnerKey)
										&& uBadge.getSystemId().equals(
												partner.getSystemId()))
									return true;
					}
			} else
				return true;
		} else
			return true;

		return false;
	}

	/**
	 * main function to generate a federated recommendation
	 * 
	 * @return
	 */
	public ResultList generateFederatedRecommendation(
			SecureUserProfile secureUserProfile) throws FileNotFoundException {
		// ResultList result = new ResultList();
		ResultList resultList = null;

		// SecureUserProfileDecomposer sUPDecomposer = null;
		// sUPDecomposer = new
		// SecureUserProfileDecomposer(federatedRecConfiguration,dbPediaSolrIndex);

		resultList = useOccurenceProbabilityPicker(secureUserProfile);
		return resultList;

	}

	/**
	 * calls the OccurrenceProbabilityPicker to aggregate results between the
	 * partners result lists
	 * 
	 * @param userProfile
	 * @return
	 */
	public ResultList useOccurenceProbabilityPicker(
			SecureUserProfile userProfile) {
		ResultList resultList;
		PartnersFederatedRecommendationsPicker pFRPicker = new OccurrenceProbabilityPicker();
		long start = System.currentTimeMillis();
		PartnersFederatedRecommendations pFR = getPartnersRecommendations(userProfile);
		long end = System.currentTimeMillis();
		long timeToGetPartners = end - start;
		start = System.currentTimeMillis();
		int numResults = 10;
		if (userProfile.numResults != null)
			numResults = userProfile.numResults;
		resultList = pFRPicker.pickResults(userProfile, pFR,
				partnerRegister.getPartners(), numResults);
		end = System.currentTimeMillis();
		long timeToPickResults = end - start;
		recommenderStats.setAverageGlobalTime(timeToGetPartners);
		recommenderStats.setAverageAggregationTime(timeToPickResults);
		logger.log(Level.INFO, " Time to get " + resultList.results.size()
				+ " Results from the Partners: " + timeToGetPartners
				+ "ms. Time to pick the best results: " + timeToPickResults
				+ "ms");
		resultList.totalResults = resultList.results.size();
		resultList.provider = "federated";
		return resultList;
	}

	/**
	 * calls the FIFOPicker to aggregate results between the partners result
	 * lists
	 * 
	 * @param userProfile
	 * @return
	 */
	public ResultList useFiFoPicker(SecureUserProfile userProfile) {
		ResultList resultList;
		PartnersFederatedRecommendationsPicker pFRPicker = new FiFoPicker();
		long start = System.currentTimeMillis();
		PartnersFederatedRecommendations pFR = getPartnersRecommendations(userProfile);
		long end = System.currentTimeMillis();
		long timeToGetPartners = end - start;
		start = System.currentTimeMillis();
		int numResults = 10;
		if (userProfile.numResults != null)
			numResults = userProfile.numResults;
		resultList = pFRPicker.pickResults(userProfile, pFR,
				partnerRegister.getPartners(), numResults);
		end = System.currentTimeMillis();
		long timeToPickResults = end - start;

		recommenderStats.setAverageGlobalTime(timeToGetPartners);
		recommenderStats.setAverageAggregationTime(timeToPickResults);
		logger.log(Level.INFO, " Time to get " + resultList.results.size()
				+ " Results from the Partners: " + timeToGetPartners
				+ "ms. Time to pick the best results: " + timeToPickResults
				+ "ms");
		resultList.totalResults = resultList.results.size();
		resultList.provider = "federated";
		return resultList;
	}

	/**
	 * calls the SimDiversificationPicker to aggregate results between the
	 * partners result lists
	 * 
	 * @param userProfile
	 * @return
	 */
	public ResultList useSimDiversificationPicker(SecureUserProfile userProfile) {
		ResultList resultList;
		SecureUserProfile currentUserProfile = new SecureUserProfile();
		PartnersFederatedRecommendationsPicker pFRPicker = new SimilarityDiversificationPicker(
				0.5);
		long start = System.currentTimeMillis();
		PartnersFederatedRecommendations pFR = getPartnersRecommendations(userProfile);
		long end = System.currentTimeMillis();
		long timeToGetPartners = end - start;
		start = System.currentTimeMillis();
		int numResults = 10;
		if (currentUserProfile.numResults != null)
			numResults = currentUserProfile.numResults;
		resultList = pFRPicker.pickResults(currentUserProfile, pFR,
				partnerRegister.getPartners(), numResults);
		end = System.currentTimeMillis();
		long timeToPickResults = end - start;
		logger.log(Level.INFO, " Time to get " + resultList.totalResults
				+ " Results from the Partners: " + timeToGetPartners
				+ "ms. Time to pick the best results: " + timeToPickResults
				+ "ms");
		resultList.totalResults = resultList.results.size();
		resultList.provider = "federated";
		return resultList;
	}

	/**
	 * calls the wikipedia query expansion
	 * 
	 * @param userProfile
	 * @return
	 */
	public SecureUserProfile generateFederatedRecommendationQEWikipedia(
			SecureUserProfileEvaluation userProfile) {
		SecureUserProfileDecomposer<SecureUserProfile, SecureUserProfile> sUPDecomposer = null;
		try {
			sUPDecomposer = new PseudoRelevanceWikipediaDecomposer(
					federatedRecConfiguration.wikipediaIndexDir,
					SUPPORTED_LOCALES);
		} catch (IOException e) {
			logger.log(Level.SEVERE,
					"Wikipedia index directory could be wrong or not readable:"
							+ federatedRecConfiguration.wikipediaIndexDir, e);
		}
		if (sUPDecomposer == null)
			return (SecureUserProfile) userProfile;
		return (SecureUserProfile) sUPDecomposer.decompose(userProfile);

	}

	/**
	 * generates a expanded user profile(query) out of the given user profile
	 * 
	 * @param userProfile
	 * @return
	 */
	public SecureUserProfile generateFederatedRecommendationQESources(
			SecureUserProfileEvaluation userProfile) {
		SecureUserProfileDecomposer<?, SecureUserProfileEvaluation> sUPDecomposer = new PseudoRelevanceSourcesDecomposer();
		return (SecureUserProfile) sUPDecomposer.decompose(userProfile);
	}

	/**
	 * generates a expanded user profile(query) out of the given user profile
	 * 
	 * @param userProfile
	 * @return
	 */
	public SecureUserProfile generateFederatedRecommendationDBPedia(
			SecureUserProfileEvaluation userProfile) {

		SecureUserProfileDecomposer<?, SecureUserProfileEvaluation> sUPDecomposer = new DBPediaDecomposer(
				federatedRecConfiguration, getDbPediaSolrIndex(),
				federatedRecConfiguration.graphQueryDepthLimit);
		return sUPDecomposer.decompose(userProfile);
	}

	/**
	 * generates a serendipitous user profile out of the given user profile
	 * 
	 * @param userProfile
	 * @return
	 */
	public SecureUserProfile generateFederatedRecommendationSerendipity(
			SecureUserProfile userProfile) {
		SecureUserProfileDecomposer<?, SecureUserProfileEvaluation> sUPDecomposer = new SerendiptiyDecomposer();
		return sUPDecomposer
				.decompose((SecureUserProfileEvaluation) userProfile);
	}

	/**
	 * TODO
	 * 
	 * @param userProfile
	 * @return
	 */
	public SecureUserProfile sourceSelectionLanguageModel(
			SecureUserProfileEvaluation userProfile) {
		// TODO add connection to langModelSourceSelection and alter
		// userProfile.partnerList to select sources
		return userProfile;
	}

	/**
	 * TODO
	 * 
	 * @param userProfile
	 * @return
	 */
	public SecureUserProfile sourceSelectionWordnet(
			SecureUserProfileEvaluation userProfile) {
		// TODO add connection to WordnetSourceSelection and alter
		// userProfile.partnerList to select sources
		return userProfile;
	}

	/**
	 * TODO
	 * 
	 * @param userProfile
	 * @return
	 */
	public SecureUserProfile sourceSelectionWikipedia(
			SecureUserProfileEvaluation userProfile) {
		// TODO add connection to WikipediaSourceSelection and alter
		// userProfile.partnerList to select sources
		return userProfile;
	}

	/**
	 * returns the path of the dbpedia solr index
	 * 
	 * @return
	 */
	public DbPediaSolrIndex getDbPediaSolrIndex() {
		return dbPediaSolrIndex;
	}

	/**
	 * returns the statistics of the federated recommender
	 * 
	 * @return
	 */
	public RecommenderStats getRecommenderStats() {
		return recommenderStats;

	}

	/**
	 * removes partners from the partner register
	 * 
	 * @param badge
	 */
	public void unregisterPartner(PartnerBadge badge) {
		synchronized (partnerRegister) {
			if (this.getPartnerRegister().getPartners().contains(badge)) {
				writeStatsToDB();
				this.getPartnerRegister().getPartners().remove(badge);
			}
		}
	}
	/**
	 * Writes the statistics for all the partners into the Database
	 */
	 public void writeStatsToDB() {

		logger.log(Level.INFO, "Writing statistics into Database");
		Database db = new Database(
				this.federatedRecConfiguration.statsLogDatabase,
				DatabaseQueryStats.values());
		for (PartnerBadge partner : this.partnerRegister.getPartners()) {
			if (partner != null) {
				PartnerBadgeStats longStats = partner.longTimeStats;
				PartnerBadgeStats shortStats = partner.shortTimeStats;
				logger.log(Level.INFO, "Writing " + partner.systemId
						+ " statistics into Database");
				PreparedStatement updateS = db
						.getPreparedUpdateStatement(DatabaseQueryStats.REQUESTLOG);
				// Database Entry Style
				// ('SYSTEM_ID','REQUESTCOUNT','FAILEDREQUESTCOUNT','FAILEDREQUESTTIMEOUTCOUNT')
				
				if (updateS != null) {
					
					try {
						updateS.clearBatch();
						updateS.setString(1, partner.getSystemId());
						updateS.setInt(2, longStats.requestCount
								+ shortStats.requestCount);
						updateS.setInt(3, longStats.failedRequestCount
								+ shortStats.failedRequestCount);
						updateS.setInt(4, longStats.failedRequestTimeoutCount
								+ shortStats.failedRequestTimeoutCount);
						updateS.execute();
					} catch (SQLException e) {
						logger.log(Level.WARNING,
								"Could not write into StatsDatabase", e);
					}finally {
							db.commit();
//						try {
//							db.close();
//						} catch (SQLException e) {
//							logger.log(Level.WARNING, "Could not close Database", e);
//						}
					}
				} else
					logger.log(Level.WARNING,
							"Could write into request statistics database");
				PreparedStatement updateQ = db
						.getPreparedUpdateStatement(DatabaseQueryStats.QUERYLOG);
				if(updateQ!=null){
				//	('ID','SYSTEM_ID','QUERY','CALLTIME','FIRSTTRANSFORMATIONTIME','SECONDTRANSFORMATIONTIME','ENRICHMENTTIME','RESULTCOUNT')
					for (ResultStats queryStats : partner.getLastQueries()) {
						try {
							updateQ.clearBatch();
							
							updateQ.setString(1,partner.getSystemId());
							updateQ.setString(2,queryStats.getPartnerQuery() );
							updateQ.setInt(3, (int) queryStats.getPartnerCallTime());
							updateQ.setInt(4, (int) queryStats.getFirstTransformationTime());
							updateQ.setInt(5, (int) queryStats.getSecondTransformationTime());
							updateQ.setInt(6, (int) queryStats.getEnrichmentTime());
							updateQ.setInt(7, queryStats.getResultCount());
							updateQ.addBatch();
							
						} catch (SQLException e) {
							logger.log(Level.WARNING,"Could not write into StatsDatabase", e);
						}
					}
					try {
						updateQ.executeBatch();
					} catch (SQLException e) {
						logger.log(Level.WARNING,"Could not write into StatsDatabase", e);
					}
					
					
				} else
					logger.log(Level.WARNING,
							"Could write into query statistics database");
			}
			}

	}
	/**
	 * Adds the partner to the partner register if not existing allready and trys to get the old
	 * statistics for this partner from the database
	 * @param badge
	 * @return
	 */
	public String registerPartner(PartnerBadge badge) {
		if (this.getPartnerRegister().getPartners().contains(badge)) {
			logger.log(Level.INFO, "Partner: " + badge.getSystemId()
					+ " allready registered!");
			synchronized (partnerRegister) {
				writeStatsToDB();	
			}
			
			return "Allready Registered";
		}

		if (badge.partnerKey != null)
			if (!badge.partnerKey.isEmpty())
				if (badge.partnerKey.length() < 20)
					return "Partner Key is too short (<20)";

		Database db = new Database(
				this.federatedRecConfiguration.statsLogDatabase,
				DatabaseQueryStats.values());
		PreparedStatement getS = db
				.getPreparedSelectStatement(DatabaseQueryStats.REQUESTLOG);
		// Database Entry Style
		// ('SYSTEM_ID','REQUESTCOUNT','FAILEDREQUESTCOUNT','FAILEDREQUESTTIMEOUTCOUNT')

		if (getS != null) {
			try {
				getS.setString(1, badge.getSystemId());
				ResultSet rs = getS.executeQuery();
				if (rs.next()) {
					badge.longTimeStats.requestCount = rs.getInt(2);
					badge.longTimeStats.failedRequestCount = rs.getInt(3);
					badge.longTimeStats.failedRequestTimeoutCount = rs
							.getInt(4);
				}
			} catch (SQLException e) {
				logger.log(
						Level.SEVERE,
						"could net get statistics for partner "
								+ badge.getSystemId(), e);
			}

			try {
				db.close();
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Could not close Database", e);
			}
		} else
			logger.log(Level.WARNING, "Could read from Statistics Database");

		this.addPartner(badge);

		return "Partner Added";

	}

}
